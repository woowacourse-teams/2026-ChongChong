package withoutc.chongchong.auth.http;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import withoutc.chongchong.auth.config.WebRefreshCookieProperties;
import withoutc.chongchong.auth.token.RawRefreshToken;

@Component
@RequiredArgsConstructor
public final class WebRefreshCookieWriter {

    private final WebRefreshCookieProperties properties;
    private final Clock clock;

    public WebRefreshCookie issue(
            RawRefreshToken refreshToken,
            Instant expiresAt
    ) {
        validateRefreshToken(refreshToken);
        Duration maxAge = calculateMaxAge(expiresAt);
        ResponseCookie responseCookie = cookieBuilder(refreshToken.value())
                .maxAge(maxAge)
                .build();

        return new WebRefreshCookie(responseCookie);
    }

    public WebRefreshCookie expire() {
        ResponseCookie responseCookie = cookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();

        return new WebRefreshCookie(responseCookie);
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String value) {
        return ResponseCookie.from(properties.name(), value)
                .secure(properties.secure())
                .httpOnly(properties.httpOnly())
                .sameSite(properties.sameSite())
                .path(properties.path());
    }

    private Duration calculateMaxAge(Instant expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("Refresh Token 만료 시각은 필수입니다.");
        }

        long remainingSeconds = Duration.between(clock.instant(), expiresAt).getSeconds();
        if (remainingSeconds <= 0) {
            throw new IllegalArgumentException("Refresh Token의 남은 유효 시간은 1초 이상이어야 합니다.");
        }
        return Duration.ofSeconds(remainingSeconds);
    }

    private void validateRefreshToken(RawRefreshToken refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("발급된 Refresh Token은 필수입니다.");
        }
    }
}
