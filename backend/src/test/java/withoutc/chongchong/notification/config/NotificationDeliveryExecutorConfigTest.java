package withoutc.chongchong.notification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class NotificationDeliveryExecutorConfigTest {

    @Test
    @DisplayName("종료 시 발송 완료를 기다리고 이후에 JPA와 데이터소스를 종료한다")
    void waitsForRunningDeliveryOnShutdown() throws Exception {
        new ApplicationContextRunner()
                .withUserConfiguration(NotificationDeliveryExecutorConfig.class, ResourceConfiguration.class)
                .withPropertyValues(
                        "web-push.delivery.send-timeout=100ms",
                        "web-push.delivery.shutdown-timeout=2s")
                .run(context -> {
                    ThreadPoolTaskExecutor executor = context.getBean(
                            "notificationDeliveryExecutor", ThreadPoolTaskExecutor.class);
                    ShutdownResource entityManagerFactory = context.getBean("entityManagerFactory", ShutdownResource.class);
                    ShutdownResource dataSource = context.getBean("dataSource", ShutdownResource.class);
                    CountDownLatch taskStarted = new CountDownLatch(1);
                    CountDownLatch finishTask = new CountDownLatch(1);
                    CountDownLatch shutdownFinished = new CountDownLatch(1);
                    AtomicBoolean taskInterrupted = new AtomicBoolean();
                    AtomicReference<Throwable> taskFailure = new AtomicReference<>();

                    executor.execute(() -> {
                        taskStarted.countDown();
                        try {
                            finishTask.await();
                            assertThat(entityManagerFactory.closed).isFalse();
                            assertThat(dataSource.closed).isFalse();
                        } catch (InterruptedException exception) {
                            taskInterrupted.set(true);
                            Thread.currentThread().interrupt();
                            taskFailure.set(exception);
                        } catch (AssertionError error) {
                            taskFailure.set(error);
                        }
                    });

                    assertThat(taskStarted.await(1, TimeUnit.SECONDS)).isTrue();
                    Thread shutdownThread = new Thread(() -> {
                        try {
                            context.close();
                        } finally {
                            shutdownFinished.countDown();
                        }
                    });
                    shutdownThread.start();

                    try {
                        assertThat(shutdownFinished.await(100, TimeUnit.MILLISECONDS)).isFalse();
                        assertThat(entityManagerFactory.closed).isFalse();
                        assertThat(dataSource.closed).isFalse();
                    } finally {
                        finishTask.countDown();
                        shutdownThread.join(1_000);
                    }

                    assertThat(shutdownFinished.getCount()).isZero();
                    assertThat(taskInterrupted).isFalse();
                    assertThat(taskFailure.get()).isNull();
                    assertThat(entityManagerFactory.closed).isTrue();
                    assertThat(dataSource.closed).isTrue();
                });
    }

    @Test
    @DisplayName("종료 대기 시간이 발송 제한 시간과 1초 여유보다 짧으면 거부한다")
    void rejectsShutdownTimeoutShorterThanSendTimeout() {
        assertThatThrownBy(() -> new NotificationDeliveryProperties(
                        1, Duration.ofSeconds(5), Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료 대기 시간은 발송 제한 시간보다 1초 이상 길어야 합니다.");
    }

    @Configuration(proxyBeanMethods = false)
    static class ResourceConfiguration {

        @Bean(destroyMethod = "close")
        ShutdownResource entityManagerFactory() {
            return new ShutdownResource();
        }

        @Bean(destroyMethod = "close")
        ShutdownResource dataSource() {
            return new ShutdownResource();
        }
    }

    static class ShutdownResource {

        private final AtomicBoolean closed = new AtomicBoolean();

        public void close() {
            closed.set(true);
        }
    }
}
