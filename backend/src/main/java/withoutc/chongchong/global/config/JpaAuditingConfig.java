package withoutc.chongchong.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@Configuration
public class JpaAuditingConfig {
}
