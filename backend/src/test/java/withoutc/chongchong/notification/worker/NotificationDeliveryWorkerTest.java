package withoutc.chongchong.notification.worker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import withoutc.chongchong.notification.config.NotificationDeliveryExecutorConfig;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.logging.NotificationDeliveryLogger;
import withoutc.chongchong.notification.metrics.NotificationDeliveryProcessingMetrics;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.sender.SimulatedNotificationSender;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryWorkerTest {

    private static final int DEFAULT_AVAILABLE_SLOTS = 1;
    private static final Long DELIVERY_ID = 1L;
    private static final Long SECOND_DELIVERY_ID = 4L;
    private static final Long THIRD_DELIVERY_ID = 5L;
    private static final Long SUBSCRIPTION_ID = 2L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private DeliveryClaimService deliveryClaimService;

    @Mock
    private DeliveryRecoveryService deliveryRecoveryService;

    @Mock
    private NotificationSender sender;

    @Mock
    private DeliveryResultService deliveryResultService;

    @Mock
    private ThreadPoolTaskExecutor notificationDeliveryExecutor;

    @Mock
    private NotificationDeliveryLogger notificationDeliveryLogger;

    private NotificationDeliveryWorker worker;
    private ClaimedDelivery delivery;
    private SimpleMeterRegistry meterRegistry;
    private final List<Runnable> submittedTasks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        NotificationDeliveryProcessingMetrics metrics = new NotificationDeliveryProcessingMetrics(meterRegistry);
        lenient().doAnswer(invocation -> {
            submittedTasks.add(invocation.getArgument(0));
            return null;
        }).when(notificationDeliveryExecutor).execute(any(Runnable.class));

        worker = new NotificationDeliveryWorker(
                deliveryClaimService,
                deliveryRecoveryService,
                deliveryResultService,
                sender,
                notificationDeliveryExecutor,
                notificationDeliveryLogger,
                metrics,
                CLOCK
        );
        delivery = new ClaimedDelivery(
                DELIVERY_ID,
                SUBSCRIPTION_ID,
                "https://push.example.com/subscription",
                "p256dh",
                "auth",
                3L,
                "제목",
                "본문",
                "/studies/1/notices/3",
                LocalDateTime.now(CLOCK).minusSeconds(10)
        );
    }

    @Test
    @DisplayName("전송 성공 시 SENT로 처리한다")
    void markSentWhenSendingSucceeds() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.SENT);

        processSingleDelivery();

        verify(deliveryResultService).markSent(DELIVERY_ID);
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("재시도 가능한 전송 실패 시 RETRY_WAIT으로 처리한다")
    void markRetryWhenSendingFailsRetryably() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.RATE_LIMITED);
        when(deliveryResultService.markRetry(
                DELIVERY_ID,
                WebPushSendResult.RATE_LIMITED.getErrorCode().getMessage()
        )).thenReturn(true);

        processSingleDelivery();

        verify(deliveryResultService).markRetry(
                DELIVERY_ID,
                WebPushSendResult.RATE_LIMITED.getErrorCode().getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("만료된 구독 응답이면 Delivery 실패와 구독 비활성화를 처리한다")
    void markExpiredWhenSubscriptionIsExpired() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.SUBSCRIPTION_EXPIRED);

        processSingleDelivery();

        verify(deliveryResultService).markExpired(
                DELIVERY_ID,
                SUBSCRIPTION_ID,
                WebPushSendResult.SUBSCRIPTION_EXPIRED.getErrorCode().getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("영구적인 전송 실패 시 FAILED로 처리한다")
    void markFailedWhenSendingFailsPermanently() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.PERMANENT_FAILURE);

        processSingleDelivery();

        verify(deliveryResultService).markFailed(
                DELIVERY_ID,
                WebPushSendResult.PERMANENT_FAILURE.getErrorCode().getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("예상하지 못한 예외는 일반 오류 메시지로 실패 처리한다")
    void markFailedWhenUnexpectedExceptionOccurs() {
        when(sender.send(delivery)).thenThrow(new IllegalStateException("unexpected"));

        processSingleDelivery();

        verify(deliveryResultService).markFailed(
                DELIVERY_ID,
                WebPushErrorCode.UNEXPECTED_WEB_PUSH_ERROR.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("전송 통신 예외는 재시도 처리하고 다음 Delivery를 진행한다")
    void markRetryWhenTransportFails() {
        WebPushException exception = new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        when(sender.send(delivery)).thenThrow(exception);
        when(deliveryResultService.markRetry(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED.getMessage()
        )).thenReturn(true);

        processSingleDelivery();

        verify(deliveryResultService).markRetry(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("중단 예외는 현재 Delivery를 재시도 처리한 뒤 다시 던진다")
    void rethrowWhenSendingIsInterrupted() {
        WebPushException exception = new WebPushException(WebPushErrorCode.WEB_PUSH_INTERRUPTED);
        when(sender.send(delivery)).thenThrow(exception);
        when(deliveryResultService.markRetry(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_INTERRUPTED.getMessage()
        )).thenReturn(true);

        scheduleSingleDelivery();

        assertThatThrownBy(this::runSubmittedTasks)
                .isSameAs(exception);

        verify(deliveryResultService).markRetry(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_INTERRUPTED.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("그 외 Web Push 예외는 영구 실패로 처리한다")
    void markFailedWhenWebPushExceptionIsNotRetryable() {
        WebPushException exception = new WebPushException(WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED);
        when(sender.send(delivery)).thenThrow(exception);

        processSingleDelivery();

        verify(deliveryResultService).markFailed(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("배치 처리 시 고착 Delivery 복구 후 claim한 Delivery를 전송한다")
    void processBatch() {
        configureMockExecutor(4, 2);
        when(deliveryClaimService.claimBatch(2)).thenReturn(List.of(delivery));
        when(sender.send(delivery)).thenReturn(WebPushSendResult.SENT);

        worker.process();
        runSubmittedTasks();

        InOrder inOrder = inOrder(
                deliveryRecoveryService, deliveryClaimService, notificationDeliveryExecutor, sender, deliveryResultService);
        inOrder.verify(deliveryRecoveryService).recover(2);
        inOrder.verify(deliveryClaimService).claimBatch(2);
        inOrder.verify(notificationDeliveryExecutor).execute(any(Runnable.class));
        inOrder.verify(sender).send(delivery);
        inOrder.verify(deliveryResultService).markSent(DELIVERY_ID);

        assertThat(meterRegistry.get("notification.delivery.claim").timer().count()).isOne();
        assertThat(meterRegistry.get("notification.delivery.queue.wait").timer().count()).isOne();
        assertThat(meterRegistry.get("notification.delivery.post.claim.wait").timer().count()).isOne();
        assertThat(meterRegistry.get("notification.delivery.send")
                .tag("outcome", "success").timer().count()).isOne();
    }

    @Test
    @DisplayName("가용 발송 스레드가 없으면 새 Delivery를 claim하지 않는다")
    void doesNotClaimWhenNoExecutorSlotsAreAvailable() {
        configureMockExecutor(2, 2);

        worker.process();

        verifyNoInteractions(deliveryRecoveryService, deliveryClaimService);
        verify(notificationDeliveryExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("일부 제출 후 executor가 거절하면 미제출 Delivery만 claim을 해제한다")
    void releasesUndispatchedDeliveriesAfterExecutorRejection() {
        configureMockExecutor(3, 0);
        ClaimedDelivery secondDelivery = createSecondDelivery();
        ClaimedDelivery thirdDelivery = createThirdDelivery();
        when(deliveryClaimService.claimBatch(3)).thenReturn(List.of(delivery, secondDelivery, thirdDelivery));
        when(sender.send(delivery)).thenReturn(WebPushSendResult.SENT);
        when(deliveryResultService.releaseClaim(SECOND_DELIVERY_ID)).thenReturn(true);
        when(deliveryResultService.releaseClaim(THIRD_DELIVERY_ID)).thenReturn(true);

        AtomicInteger submissions = new AtomicInteger();
        doAnswer(invocation -> {
            if (submissions.incrementAndGet() == 2) {
                throw new TaskRejectedException("executor is shutting down");
            }
            submittedTasks.add(invocation.getArgument(0));
            return null;
        }).when(notificationDeliveryExecutor).execute(any(Runnable.class));

        worker.process();
        runSubmittedTasks();

        verify(sender).send(delivery);
        verify(sender, never()).send(secondDelivery);
        verify(sender, never()).send(thirdDelivery);
        verify(deliveryResultService).markSent(DELIVERY_ID);
        verify(deliveryResultService, never()).markRetry(eq(DELIVERY_ID), any(String.class));
        verify(deliveryResultService).releaseClaim(SECOND_DELIVERY_ID);
        verify(deliveryResultService).releaseClaim(THIRD_DELIVERY_ID);
        verify(deliveryResultService, never()).releaseClaim(DELIVERY_ID);
        verify(deliveryResultService, never()).markRetry(eq(SECOND_DELIVERY_ID), any(String.class));
        verify(deliveryResultService, never()).markRetry(eq(THIRD_DELIVERY_ID), any(String.class));
        verify(notificationDeliveryLogger).submissionRejected(
                eq(secondDelivery), eq(true), any(TaskRejectedException.class));
        verify(notificationDeliveryLogger).submissionRejected(
                eq(thirdDelivery), eq(true), any(TaskRejectedException.class));
        assertThat(meterRegistry.get("notification.delivery.queue.wait").timer().count()).isOne();
    }

    @Test
    @DisplayName("설정한 가용 슬롯 수만큼 실제 발송을 동시에 실행한다")
    void sendsDeliveriesConcurrentlyUpToAvailableSlots() throws InterruptedException {
        ThreadPoolTaskExecutor executor = new NotificationDeliveryExecutorConfig()
                .notificationDeliveryExecutor(2);
        executor.initialize();

        CountDownLatch sendsStarted = new CountDownLatch(2);
        CountDownLatch releaseSends = new CountDownLatch(1);
        AtomicInteger activeSends = new AtomicInteger();
        AtomicInteger peakActiveSends = new AtomicInteger();
        NotificationSender blockingSender = claimedDelivery -> {
            int active = activeSends.incrementAndGet();
            peakActiveSends.accumulateAndGet(active, Math::max);
            sendsStarted.countDown();
            try {
                if (!releaseSends.await(5, TimeUnit.SECONDS)) {
                    return WebPushSendResult.PERMANENT_FAILURE;
                }
                return WebPushSendResult.SENT;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return WebPushSendResult.PERMANENT_FAILURE;
            } finally {
                activeSends.decrementAndGet();
            }
        };
        NotificationDeliveryWorker concurrentWorker = new NotificationDeliveryWorker(
                deliveryClaimService,
                deliveryRecoveryService,
                deliveryResultService,
                blockingSender,
                executor,
                notificationDeliveryLogger,
                new NotificationDeliveryProcessingMetrics(meterRegistry),
                CLOCK
        );
        when(deliveryClaimService.claimBatch(2)).thenReturn(List.of(delivery, createSecondDelivery()));

        try {
            concurrentWorker.process();

            assertThat(sendsStarted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(peakActiveSends.get()).isEqualTo(2);
        } finally {
            releaseSends.countDown();
            executor.getThreadPoolExecutor().shutdown();
            assertThat(executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("시뮬레이터를 사용한 20건도 기존 worker의 성공 반영과 알림 메트릭을 거친다")
    void processSimulatedBatchThroughExistingResultAndMetrics() {
        configureMockExecutor(20, 0);
        worker = new NotificationDeliveryWorker(
                deliveryClaimService,
                deliveryRecoveryService,
                deliveryResultService,
                new SimulatedNotificationSender(0),
                notificationDeliveryExecutor,
                notificationDeliveryLogger,
                new NotificationDeliveryProcessingMetrics(meterRegistry),
                CLOCK
        );
        List<ClaimedDelivery> deliveries = LongStream.rangeClosed(1, 20)
                .mapToObj(id -> new ClaimedDelivery(
                        id, id, "https://load-test.invalid/subscriptions/" + id, "synthetic-p256dh", "synthetic-auth",
                        id, "제목", "본문", "/studies/1/notices/3", LocalDateTime.now(CLOCK).minusSeconds(10)
                ))
                .toList();
        when(deliveryClaimService.claimBatch(20)).thenReturn(deliveries);

        worker.process();
        runSubmittedTasks();

        for (ClaimedDelivery claimed : deliveries) {
            verify(deliveryResultService).markSent(claimed.id());
        }
        verifyNoMoreInteractions(deliveryResultService);
        assertThat(meterRegistry.get("notification.delivery.queue.wait").timer().count()).isEqualTo(20);
        assertThat(meterRegistry.get("notification.delivery.post.claim.wait").timer().count()).isEqualTo(20);
        assertThat(meterRegistry.get("notification.delivery.send")
                .tag("outcome", "success").timer().count()).isEqualTo(20);
    }

    private void processSingleDelivery() {
        scheduleSingleDelivery();
        runSubmittedTasks();
    }

    private void scheduleSingleDelivery() {
        configureMockExecutor(DEFAULT_AVAILABLE_SLOTS, 0);
        when(deliveryClaimService.claimBatch(DEFAULT_AVAILABLE_SLOTS)).thenReturn(List.of(delivery));
        worker.process();
    }

    private void configureMockExecutor(int maxPoolSize, int activeCount) {
        when(notificationDeliveryExecutor.getMaxPoolSize()).thenReturn(maxPoolSize);
        when(notificationDeliveryExecutor.getActiveCount()).thenReturn(activeCount);
    }

    private void runSubmittedTasks() {
        List<Runnable> tasks = List.copyOf(submittedTasks);
        submittedTasks.clear();
        tasks.forEach(Runnable::run);
    }

    private ClaimedDelivery createSecondDelivery() {
        return new ClaimedDelivery(
                SECOND_DELIVERY_ID,
                SUBSCRIPTION_ID + 1,
                "https://push.example.com/second-subscription",
                "p256dh",
                "auth",
                4L,
                "두 번째 제목",
                "두 번째 본문",
                "/studies/1/notices/4",
                LocalDateTime.now(CLOCK).minusSeconds(10)
        );
    }

    private ClaimedDelivery createThirdDelivery() {
        return new ClaimedDelivery(
                THIRD_DELIVERY_ID,
                SUBSCRIPTION_ID + 2,
                "https://push.example.com/third-subscription",
                "p256dh",
                "auth",
                5L,
                "세 번째 제목",
                "세 번째 본문",
                "/studies/1/notices/5",
                LocalDateTime.now(CLOCK).minusSeconds(10)
        );
    }

}
