package withoutc.chongchong.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.web.csrf.CsrfToken;

public record WebCsrfTokenResponse(
        @Schema(description = "CSRF 토큰을 전송할 HTTP 헤더 이름", example = "X-XSRF-TOKEN")
        String headerName,
        @Schema(description = "CSRF 토큰")
        String token
) {

    public static WebCsrfTokenResponse from(CsrfToken csrfToken) {
        return new WebCsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @Override
    public String toString() {
        return "WebCsrfTokenResponse[headerName=" + headerName + ", token=REDACTED]";
    }
}
