package withoutc.chongchong.auth.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.refresh-token")
public record RefreshTokenProperties(
        @NotNull Duration validity
) {

    public RefreshTokenProperties {
        if (validity == null || validity.isZero() || validity.isNegative()) {
            throw new IllegalArgumentException("Refresh Token 유효 시간은 0보다 커야 합니다.");
        }
    }
}
