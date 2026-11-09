package withoutc.chongchong.notification.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(NotificationDeliveryProperties.class)
public class NotificationDeliveryExecutorConfig {

    private static final String THREAD_NAME_PREFIX = "notification-send-";

    @Bean(defaultCandidate = false)
    @Qualifier("notificationDeliveryExecutor")
    @DependsOn({"entityManagerFactory", "dataSource"})
    public ThreadPoolTaskExecutor notificationDeliveryExecutor(NotificationDeliveryProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(properties.concurrency());
        // 대기열을 만들지 않음. 스레드가 처리할 수 있을 만큼만 claim 해야 함
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationMillis(properties.shutdownTimeout().toMillis());

        return executor;
    }

    // 발송 worker 전용 스케줄러. 일이 밀리면 worker가 한 번에 오래 돌 수 있어 공용 스케줄러와 분리
    @Bean(defaultCandidate = false)
    @Qualifier("notificationDeliveryScheduler")
    public ThreadPoolTaskScheduler notificationDeliveryScheduler(NotificationDeliveryProperties properties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("notification-delivery-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationMillis(properties.shutdownTimeout().toMillis());
        return scheduler;
    }
}
