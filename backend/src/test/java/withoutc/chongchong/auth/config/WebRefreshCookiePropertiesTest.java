package withoutc.chongchong.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class WebRefreshCookiePropertiesTest {

    @Test
    @DisplayName("웹 Refresh Cookie의 확정된 속성을 허용한다")
    void allowValidProperties() {
        WebRefreshCookieProperties properties = properties(true);

        assertThat(properties.name()).isEqualTo("refresh_token");
        assertThat(properties.secure()).isTrue();
        assertThat(properties.httpOnly()).isTrue();
        assertThat(properties.sameSite()).isEqualTo("Lax");
        assertThat(properties.path()).isEqualTo("/auth");
    }

    @Test
    @DisplayName("환경 정책을 적용하기 전 Secure 비활성화 설정값을 보관한다")
    void holdInsecurePropertiesBeforeEnvironmentPolicyValidation() {
        WebRefreshCookieProperties properties = properties(false);

        assertThat(properties.secure()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "refresh token", "refresh_token;Path=/"})
    @DisplayName("비어 있거나 형식이 잘못된 Cookie 이름을 거부한다")
    void rejectInvalidName(String name) {
        assertThatThrownBy(() -> new WebRefreshCookieProperties(
                name,
                true,
                true,
                "Lax",
                "/auth"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("누락된 Secure 설정을 거부한다")
    void rejectNullSecure() {
        assertThatThrownBy(() -> new WebRefreshCookieProperties(
                "refresh_token",
                null,
                true,
                "Lax",
                "/auth"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Cookie Secure 설정은 필수입니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName("HttpOnly가 아닌 설정을 거부한다")
    void rejectNotHttpOnly(Boolean httpOnly) {
        assertThatThrownBy(() -> new WebRefreshCookieProperties(
                "refresh_token",
                true,
                httpOnly,
                "Lax",
                "/auth"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Cookie는 HttpOnly여야 합니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"lax", "Strict", "None"})
    @DisplayName("합의한 Lax가 아닌 SameSite 설정을 거부한다")
    void rejectUnsupportedSameSite(String sameSite) {
        assertThatThrownBy(() -> new WebRefreshCookieProperties(
                "refresh_token",
                true,
                true,
                sameSite,
                "/auth"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Cookie SameSite는 Lax여야 합니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/", "/api", "auth"})
    @DisplayName("합의한 /auth가 아닌 Cookie Path를 거부한다")
    void rejectUnsupportedPath(String path) {
        assertThatThrownBy(() -> new WebRefreshCookieProperties(
                "refresh_token",
                true,
                true,
                "Lax",
                path
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Cookie Path는 /auth여야 합니다.");
    }

    private WebRefreshCookieProperties properties(boolean secure) {
        return new WebRefreshCookieProperties(
                "refresh_token",
                secure,
                true,
                "Lax",
                "/auth"
        );
    }
}
