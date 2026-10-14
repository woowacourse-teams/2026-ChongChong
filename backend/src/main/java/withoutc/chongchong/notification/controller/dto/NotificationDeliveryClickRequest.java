package withoutc.chongchong.notification.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationDeliveryClickRequest(
        @NotBlank(message = "푸시 클릭 토큰은 필수입니다.")
        String clickToken
) {
}
