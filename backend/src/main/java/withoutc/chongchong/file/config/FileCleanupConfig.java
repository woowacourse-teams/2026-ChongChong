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
        // 종료 때 하던 배치는 기본 스케줄러와 같은 종료 단계에서 함께 기다린다. 따로 기다리면 종료 시간에 그만큼 더해진다.
        return scheduler;
    }
}
