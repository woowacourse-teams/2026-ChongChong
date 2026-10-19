package withoutc.chongchong.notification.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "web-push.sender")
public record NotificationSenderProperties(
        @DefaultValue("real") Mode mode,
        @DefaultValue("500") @Min(0) long simulatedDelayMs
) {

    public enum Mode {
        REAL,
        SIMULATED
    }
}
