package withoutc.chongchong.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KakaoLoginProperties.class)
public class KakaoLoginConfig {
}
