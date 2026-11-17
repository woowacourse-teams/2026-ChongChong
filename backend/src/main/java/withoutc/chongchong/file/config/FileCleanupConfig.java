package withoutc.chongchong.file.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(FileCleanupProperties.class)
public class FileCleanupConfig {

    public FileCleanupConfig(
            FileCleanupProperties properties,
            @Value("${aws.s3.url-validity}") Duration uploadUrlValidity
    ) {
        properties.validateAgainstUploadUrlValidity(uploadUrlValidity);
    }

    @Bean(defaultCandidate = false)
    @Qualifier("fileCleanupScheduler")
    public ThreadPoolTaskScheduler fileCleanupScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("file-cleanup-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationMillis(Duration.ofSeconds(30).toMillis());
        return scheduler;
    }
}
