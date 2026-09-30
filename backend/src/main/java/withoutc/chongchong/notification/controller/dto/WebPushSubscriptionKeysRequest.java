package withoutc.chongchong.notification.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebPushSubscriptionKeysRequest(
        @Size(max = 255, message = "p256dh 키는 255자 이내여야 합니다.")
        @NotBlank(message = "p256dh 키는 필수입니다.")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "p256dh 키 형식이 올바르지 않습니다.")
        String p256dh,

        @Size(max = 255, message = "auth 키는 255자 이내여야 합니다.")
        @NotBlank(message = "auth 키는 필수입니다.")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "auth 키 형식이 올바르지 않습니다.")
        String auth
) {
}
