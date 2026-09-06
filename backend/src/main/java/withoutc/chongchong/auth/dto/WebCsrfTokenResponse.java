package withoutc.chongchong.auth.dto;

import org.springframework.security.web.csrf.CsrfToken;

public record WebCsrfTokenResponse(
        String headerName,
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
