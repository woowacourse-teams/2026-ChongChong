package withoutc.chongchong.auth.http;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import withoutc.chongchong.auth.config.WebRefreshCookieProperties;
import withoutc.chongchong.auth.token.RawRefreshToken;

@Component
@RequiredArgsConstructor
public final class WebRefreshCookieReader {

    private final WebRefreshCookieProperties properties;

    public Optional<RawRefreshToken> read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.name());
        if (cookie == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(new RawRefreshToken(cookie.getValue()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
