package withoutc.chongchong.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class NotificationDeliveryExecutorConfig {

    private static final String THREAD_NAME_PREFIX = "notification-send-";

    @Bean
    public ThreadPoolTaskExecutor notificationDeliveryExecutor(
            @Value("${web-push.delivery.concurrency:1}") int concurrency
    ) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("푸시 알림 동시 발송 수는 1 이상이어야 합니다.");
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(concurrency);
        // 대기열을 만들지 않음. 스레드가 처리할 수 있을 만큼만 claim 해야 함
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);

        return executor;
    }
}
