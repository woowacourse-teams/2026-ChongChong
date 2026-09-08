package withoutc.chongchong.notification.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import withoutc.chongchong.notification.entity.DevicePlatform;

public record PushTokenCreateRequest(
        @Size(max = 255, message = "설치된 앱 식별자는 255자 이내여야 합니다.")
        @NotBlank(message = "설치된 앱 식별자는 필수입니다.")
        String installationId,

        @Size(max = 255, message = "푸시 토큰은 255자 이내여야 합니다.")
        @NotBlank(message = "푸시 토큰은 필수입니다.")
        String token,

        @NotNull(message = "디바이스 플랫폼은 필수입니다.")
        DevicePlatform platform
) {
}
