package withoutc.chongchong.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    @Bean
    public ValidationConfigurationCustomizer validationClockCustomizer(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
