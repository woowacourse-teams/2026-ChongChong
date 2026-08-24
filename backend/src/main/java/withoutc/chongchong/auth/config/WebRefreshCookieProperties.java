package withoutc.chongchong.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.web.refresh-cookie")
public record WebRefreshCookieProperties(
        @NotBlank String name,
        @NotNull Boolean secure,
        @NotNull Boolean httpOnly,
        @NotBlank String sameSite,
        @NotBlank String path
) {

    private static final Pattern COOKIE_NAME_PATTERN = Pattern.compile("[!#$%&'*+\\-.^_`|~0-9A-Za-z]+");
    private static final String REQUIRED_SAME_SITE = "Lax";
    private static final String REQUIRED_PATH = "/auth";

    public WebRefreshCookieProperties {
        validateName(name);
        validateSecure(secure);
        validateHttpOnly(httpOnly);
        validateSameSite(sameSite);
        validatePath(path);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Refresh Cookie 이름은 비어 있을 수 없습니다.");
        }
        if (!COOKIE_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Refresh Cookie 이름 형식이 올바르지 않습니다.");
        }
    }

    private void validateSecure(Boolean secure) {
        if (secure == null) {
            throw new IllegalArgumentException("Refresh Cookie Secure 설정은 필수입니다.");
        }
    }

    private void validateHttpOnly(Boolean httpOnly) {
        if (!Boolean.TRUE.equals(httpOnly)) {
            throw new IllegalArgumentException("Refresh Cookie는 HttpOnly여야 합니다.");
        }
    }

    private void validateSameSite(String sameSite) {
        if (!REQUIRED_SAME_SITE.equals(sameSite)) {
            throw new IllegalArgumentException("Refresh Cookie SameSite는 Lax여야 합니다.");
        }
    }

    private void validatePath(String path) {
        if (!REQUIRED_PATH.equals(path)) {
            throw new IllegalArgumentException("Refresh Cookie Path는 /auth여야 합니다.");
        }
    }
}
