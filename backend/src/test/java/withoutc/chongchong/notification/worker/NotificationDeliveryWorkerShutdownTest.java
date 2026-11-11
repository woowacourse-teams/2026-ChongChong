package withoutc.chongchong.notification.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import withoutc.chongchong.notification.config.NotificationDeliveryExecutorConfig;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.logging.NotificationDeliveryLogger;
import withoutc.chongchong.notification.metrics.NotificationDeliveryProcessingMetrics;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

class NotificationDeliveryWorkerShutdownTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("일이 밀린 상태에서 종료하면 이어서 claim하던 반복이 멈추고, 발송 중인 작업은 결과를 남긴 뒤 리소스가 닫힌다")
    void shutsDownInOrderWithoutLeavingClaims() {
        Scenario.reset();
        new ApplicationContextRunner()
                .withUserConfiguration(NotificationDeliveryExecutorConfig.class, Scenario.class)
                .withPropertyValues(
                        "web-push.delivery.concurrency=2",
                        "web-push.delivery.fixed-delay-ms=100",
                        "web-push.delivery.send-timeout=100ms",
                        "web-push.delivery.shutdown-timeout=3s")
                .run(context -> {
                    assertThat(Scenario.sendsStarted.await(3, TimeUnit.SECONDS)).isTrue();
                    // 발송 worker는 전용 스케줄러 스레드에서 돈다.
                    assertThat(Scenario.claimThreadName).startsWith("notification-delivery-scheduler-");

                    Thread closer = new Thread(context::close);
                    closer.start();
                    closer.join(300);
                    // 발송 중인 2건이 끝나기 전에는 종료가 끝나지 않고 리소스도 열려 있어야 한다.
                    assertThat(closer.isAlive()).isTrue();
                    assertThat(Scenario.resourceClosed).isFalse();
                    int claimsWhenClosing = Scenario.claimCalls.get();

                    Scenario.releaseSends.countDown();
                    closer.join(3_000);

                    assertThat(closer.isAlive()).isFalse();
                    assertThat(Scenario.claimCalls.get()).isEqualTo(claimsWhenClosing);
                    assertThat(Scenario.resourceClosed).isTrue();
                    // claim한 Delivery는 모두 SENT로 기록되어 PROCESSING으로 남지 않는다.
                    assertThat(Scenario.markedSent.get()).isEqualTo(Scenario.claimed.get());
                    assertThat(Scenario.sentBeforeResourceClosed).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class Scenario {

        static AtomicInteger claimCalls;
        static AtomicInteger claimed;
        static AtomicInteger markedSent;
        static CountDownLatch sendsStarted;
        static CountDownLatch releaseSends;
        static volatile String claimThreadName;
        static volatile boolean resourceClosed;
        static volatile boolean sentBeforeResourceClosed;

        static void reset() {
            claimCalls = new AtomicInteger();
            claimed = new AtomicInteger();
            markedSent = new AtomicInteger();
            sendsStarted = new CountDownLatch(2);
            releaseSends = new CountDownLatch(1);
            claimThreadName = "";
            resourceClosed = false;
            sentBeforeResourceClosed = false;
        }

        @Bean(destroyMethod = "close")
        ShutdownResource entityManagerFactory() {
            return new ShutdownResource();
        }

        @Bean(destroyMethod = "close")
        ShutdownResource dataSource() {
            return new ShutdownResource();
        }

        @Bean
        NotificationDeliveryWorker notificationDeliveryWorker(
                @Qualifier("notificationDeliveryExecutor") ThreadPoolTaskExecutor executor
        ) {
            DeliveryClaimService claimService = mock(DeliveryClaimService.class);
            // 큐에 일이 계속 있는 상황: 요청한 수만큼 항상 돌려준다.
            when(claimService.claimBatch(anyInt())).thenAnswer(invocation -> {
                claimCalls.incrementAndGet();
                claimThreadName = Thread.currentThread().getName();
                int batchSize = invocation.getArgument(0);
                List<ClaimedDelivery> deliveries = new ArrayList<>();
                for (int i = 0; i < batchSize; i++) {
                    long id = claimed.incrementAndGet();
                    deliveries.add(new ClaimedDelivery(id, id, "https://mock-push/push/" + id, "p256dh", "auth",
                            id, "제목", "본문", "/", LocalDateTime.now(CLOCK)));
                }
                return deliveries;
            });
            DeliveryResultService resultService = mock(DeliveryResultService.class);
            doAnswer(invocation -> {
                markedSent.incrementAndGet();
                sentBeforeResourceClosed = !resourceClosed;
                return null;
            }).when(resultService).markSent(any());
            NotificationSender sender = delivery -> {
                sendsStarted.countDown();
                try {
                    releaseSends.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return WebPushSendResult.SENT;
            };
            return new NotificationDeliveryWorker(
                    claimService,
                    mock(DeliveryRecoveryService.class),
                    resultService,
                    sender,
                    executor,
                    mock(NotificationDeliveryLogger.class),
                    new NotificationDeliveryProcessingMetrics(new SimpleMeterRegistry()),
                    CLOCK
            );
        }
    }

    static class ShutdownResource {

        public void close() {
            Scenario.resourceClosed = true;
        }
    }
}
