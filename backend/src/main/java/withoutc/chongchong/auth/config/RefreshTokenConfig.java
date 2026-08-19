package withoutc.chongchong.auth.config;

import java.security.SecureRandom;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class RefreshTokenConfig {

    public static final String REFRESH_TOKEN_SECURE_RANDOM = "refreshTokenSecureRandom";

    @Bean(REFRESH_TOKEN_SECURE_RANDOM)
    SecureRandom refreshTokenSecureRandom() {
        return new SecureRandom();
    }
}
