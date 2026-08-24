package withoutc.chongchong.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class WebRefreshCookieConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(WebRefreshCookieConfig.class)
            .withPropertyValues(
                    "auth.web.refresh-cookie.name=refresh_token",
                    "auth.web.refresh-cookie.secure=true",
                    "auth.web.refresh-cookie.http-only=true",
                    "auth.web.refresh-cookie.same-site=Lax",
                    "auth.web.refresh-cookie.path=/auth"
            );

    @Test
    @DisplayName("웹 Refresh Cookie 설정을 애플리케이션 설정으로 등록한다")
    void registerProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebRefreshCookieProperties.class);
        });
    }

    @Test
    @DisplayName("HttpOnly가 비활성화된 설정은 애플리케이션 시작을 실패시킨다")
    void rejectNotHttpOnlyOnStartup() {
        contextRunner
                .withPropertyValues("auth.web.refresh-cookie.http-only=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("Refresh Cookie는 HttpOnly여야 합니다.");
                });
    }
}
