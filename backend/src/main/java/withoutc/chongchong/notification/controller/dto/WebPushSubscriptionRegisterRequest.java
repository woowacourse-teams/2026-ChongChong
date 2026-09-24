package withoutc.chongchong.notification.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebPushSubscriptionRegisterRequest(
        @Size(max = 2048, message = "Web Push endpoint는 2,048자 이내여야 합니다.")
        @NotBlank(message = "Web Push endpoint는 필수입니다.")
        @Pattern(regexp = "https://\\S+", message = "Web Push endpoint는 HTTPS URL이어야 합니다.")
        String endpoint,

        @NotNull(message = "Web Push keys는 필수입니다.")
        @Valid
        WebPushSubscriptionKeysRequest keys
) {
}
