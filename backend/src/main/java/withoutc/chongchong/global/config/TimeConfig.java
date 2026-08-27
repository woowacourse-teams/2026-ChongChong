package withoutc.chongchong.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }

    @Bean
    public ValidationConfigurationCustomizer validationClockCustomizer(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
