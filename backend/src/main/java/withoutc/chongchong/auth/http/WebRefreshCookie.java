package withoutc.chongchong.auth.http;

import java.util.Objects;
import org.springframework.http.ResponseCookie;

public final class WebRefreshCookie {

    private final ResponseCookie responseCookie;

    WebRefreshCookie(ResponseCookie responseCookie) {
        this.responseCookie = Objects.requireNonNull(responseCookie);
    }

    public String headerValue() {
        return responseCookie.toString();
    }

    @Override
    public String toString() {
        return "WebRefreshCookie[value=REDACTED]";
    }
}
