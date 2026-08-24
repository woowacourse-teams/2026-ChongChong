package withoutc.chongchong.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class WebCsrfTokenResponseTest {

    @Test
    @DisplayName("CSRF 응답의 문자열 표현에는 Token 원문을 노출하지 않는다")
    void redactTokenFromStringRepresentation() {
        String token = "test-csrf-token";
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", token);

        WebCsrfTokenResponse response = WebCsrfTokenResponse.from(csrfToken);

        assertThat(response.headerName()).isEqualTo("X-XSRF-TOKEN");
        assertThat(response.token()).isEqualTo(token);
        assertThat(response.toString())
                .contains("token=REDACTED")
                .doesNotContain(token);
    }
}
