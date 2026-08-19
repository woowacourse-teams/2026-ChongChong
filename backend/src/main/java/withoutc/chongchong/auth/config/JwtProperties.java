package withoutc.chongchong.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotBlank String secretBase64,
        @NotNull Duration accessTokenValidity
) {

    public JwtProperties {
        if (accessTokenValidity == null
                || accessTokenValidity.isZero()
                || accessTokenValidity.isNegative()) {
            throw new IllegalArgumentException("Access Token 유효 시간은 0보다 커야 합니다.");
        }
    }
}
