package withoutc.chongchong.auth.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.auth.config.WebRefreshCookieProperties;
import withoutc.chongchong.auth.token.RawRefreshToken;

class WebRefreshCookieWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-24T01:00:00Z");
    private static final String REFRESH_TOKEN = "sensitive-refresh-token";

    private final WebRefreshCookieWriter cookieWriter = new WebRefreshCookieWriter(
            properties(true),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    @DisplayName("Refresh Token과 남은 만료 시간으로 보안 Cookie를 생성한다")
    void issueSecureRefreshCookie() {
        WebRefreshCookie cookie = cookieWriter.issue(
                new RawRefreshToken(REFRESH_TOKEN),
                NOW.plusSeconds(3_600)
        );

        assertThat(cookie.headerValue())
                .contains("refresh_token=" + REFRESH_TOKEN)
                .contains("Max-Age=3600")
                .contains("Path=/auth")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Domain=");
    }

    @Test
    @DisplayName("Cookie Max-Age가 서버 만료 시각을 넘지 않도록 초 단위로 내림한다")
    void truncateMaxAgeToSeconds() {
        WebRefreshCookie cookie = cookieWriter.issue(
                new RawRefreshToken(REFRESH_TOKEN),
                NOW.plusSeconds(3_600).plusNanos(999_999_999)
        );

        assertThat(cookie.headerValue())
                .contains("Max-Age=3600")
                .doesNotContain("Max-Age=3601");
    }

    @Test
    @DisplayName("발급 Cookie와 같은 범위의 만료 Cookie를 생성한다")
    void expireRefreshCookieWithSameScope() {
        WebRefreshCookie cookie = cookieWriter.expire();

        assertThat(cookie.headerValue())
                .contains("refresh_token=")
                .contains("Max-Age=0")
                .contains("Path=/auth")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Domain=");
    }

    @Test
    @DisplayName("Cookie 문자열 표현에 Refresh Token 원문을 노출하지 않는다")
    void redactRefreshTokenFromStringRepresentation() {
        WebRefreshCookie cookie = cookieWriter.issue(
                new RawRefreshToken(REFRESH_TOKEN),
                NOW.plusSeconds(3_600)
        );

        assertThat(cookie.toString())
                .contains("value=REDACTED")
                .doesNotContain(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token이 누락되면 Cookie를 생성하지 않는다")
    void rejectNullRefreshToken() {
        assertThatThrownBy(() -> cookieWriter.issue(null, NOW.plusSeconds(3_600)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("발급된 Refresh Token은 필수입니다.");
    }

    @Test
    @DisplayName("Refresh Token 만료 시각이 누락되면 Cookie를 생성하지 않는다")
    void rejectNullExpiresAt() {
        assertThatThrownBy(() -> cookieWriter.issue(new RawRefreshToken(REFRESH_TOKEN), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token 만료 시각은 필수입니다.");
    }

    @Test
    @DisplayName("남은 유효 시간이 1초 미만이면 Cookie를 생성하지 않는다")
    void rejectNonPositiveMaxAge() {
        assertThatThrownBy(() -> cookieWriter.issue(
                new RawRefreshToken(REFRESH_TOKEN),
                NOW.plusNanos(999_999_999)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token의 남은 유효 시간은 1초 이상이어야 합니다.");
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
