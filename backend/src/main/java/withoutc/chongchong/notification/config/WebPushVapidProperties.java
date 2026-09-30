package withoutc.chongchong.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "web-push.vapid")
public record WebPushVapidProperties(
        @NotBlank String publicKey,
        @NotBlank String privateKey,
        @NotBlank String subject
) {
}
