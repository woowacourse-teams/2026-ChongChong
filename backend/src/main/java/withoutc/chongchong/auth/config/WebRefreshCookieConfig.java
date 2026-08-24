package withoutc.chongchong.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebRefreshCookieProperties.class)
public class WebRefreshCookieConfig {
}
