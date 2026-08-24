package withoutc.chongchong.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.social.kakao")
public record KakaoLoginProperties(
        @NotBlank String restApiKey,
        @NotBlank String clientSecret,
        @NotNull URI redirectUri,
        @NotNull URI tokenUri,
        @NotNull URI userInfoUri,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {

    public KakaoLoginProperties {
        validateNotBlank("Kakao REST API 키는", restApiKey);
        validateNotBlank("Kakao Client Secret은", clientSecret);
        validateSecureHttpUri("Kakao Redirect URI", redirectUri);
        validateSecureHttpUri("Kakao Token URI", tokenUri);
        validateSecureHttpUri("Kakao 사용자 정보 URI", userInfoUri);
        validatePositiveDuration("Kakao 연결 제한 시간", connectTimeout);
        validatePositiveDuration("Kakao 응답 제한 시간", readTimeout);
    }

    private void validateNotBlank(String subject, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(subject + " 비어 있을 수 없습니다.");
        }
    }

    private void validateSecureHttpUri(String name, URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getHost() == null || !isSecureHttpUri(uri)) {
            throw new IllegalArgumentException(name + "는 HTTPS 또는 loopback HTTP URI여야 합니다.");
        }
    }

    private boolean isSecureHttpUri(URI uri) {
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return true;
        }
        return "http".equalsIgnoreCase(uri.getScheme()) && isLoopbackHost(uri.getHost());
    }

    private boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

    private void validatePositiveDuration(String name, Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + "은 0보다 커야 합니다.");
        }
    }

    @Override
    public String toString() {
        return "KakaoLoginProperties[restApiKey=REDACTED, clientSecret=REDACTED, redirectUri="
                + redirectUri + ", tokenUri=" + tokenUri + ", userInfoUri=" + userInfoUri
                + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout + "]";
    }
}
