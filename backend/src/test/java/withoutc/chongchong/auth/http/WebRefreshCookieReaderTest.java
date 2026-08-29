package withoutc.chongchong.auth.http;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import withoutc.chongchong.auth.config.WebRefreshCookieProperties;

class WebRefreshCookieReaderTest {

    private final WebRefreshCookieReader cookieReader = new WebRefreshCookieReader(
            new WebRefreshCookieProperties("refresh_token", true, true, "Lax", "/api/auth")
    );

    @Test
    @DisplayName("설정한 이름의 Cookie에서 Refresh Token 원문을 읽는다")
    void readRefreshToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "raw-refresh-token"));

        assertThat(cookieReader.read(request))
                .hasValueSatisfying(refreshToken -> assertThat(refreshToken.value())
                        .isEqualTo("raw-refresh-token"));
    }

    @Test
    @DisplayName("Refresh Cookie가 없으면 빈 결과를 반환한다")
    void returnEmptyWhenRefreshCookieIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(cookieReader.read(request)).isEmpty();
    }

    @Test
    @DisplayName("Refresh Cookie 값이 비어 있으면 빈 결과를 반환한다")
    void returnEmptyWhenRefreshCookieIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", " "));

        assertThat(cookieReader.read(request)).isEmpty();
    }
}
