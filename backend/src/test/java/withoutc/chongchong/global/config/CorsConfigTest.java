package withoutc.chongchong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CorsConfig.class)
            .withPropertyValues(
                    "frontend.base-url=https://fallback.example",
                    "frontend.allowed-origins=${frontend.base-url}"
            );

    @Test
    @DisplayName("CORS Origin 목록을 생략하면 프론트엔드 기준 URL을 허용한다")
    void useFrontendBaseUrlAsAllowedOriginFallback() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            CorsConfigurationSource source = context.getBean(CorsConfigurationSource.class);
            CorsConfiguration configuration = source.getCorsConfiguration(
                    new MockHttpServletRequest("GET", "/")
            );

            assertThat(configuration).isNotNull();
            assertThat(configuration.getAllowedOrigins()).isEqualTo(List.of("https://fallback.example"));
        });
    }
}
