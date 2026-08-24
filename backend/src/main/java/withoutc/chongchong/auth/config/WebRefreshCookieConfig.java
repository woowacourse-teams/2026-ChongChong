package withoutc.chongchong.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@EnableConfigurationProperties(WebRefreshCookieProperties.class)
public class WebRefreshCookieConfig {

    private static final Profiles LOCAL_PROFILE = Profiles.of("local");

    public WebRefreshCookieConfig(
            WebRefreshCookieProperties properties,
            Environment environment
    ) {
        validateSecurePolicy(properties, environment);
    }

    private void validateSecurePolicy(
            WebRefreshCookieProperties properties,
            Environment environment
    ) {
        if (!properties.secure() && !environment.acceptsProfiles(LOCAL_PROFILE)) {
            throw new IllegalStateException("Refresh Cookie Secure 비활성화는 local 프로필에서만 허용됩니다.");
        }
    }
}
