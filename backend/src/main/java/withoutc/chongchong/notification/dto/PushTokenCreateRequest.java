package withoutc.chongchong.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.TokenProvider;

public record PushTokenCreateRequest(
        @NotBlank(message = "설치된 앱 식별자는 필수입니다.")
        @Schema(description = "설치된 앱 식별자", example = "installation-1")
        String installationId,

        @NotNull(message = "푸시 토큰 제공자는 필수입니다.")
        @Schema(description = "푸시 토큰 제공자", example = "EXPO")
        TokenProvider provider,

        @NotBlank(message = "푸시 토큰은 필수입니다.")
        @Schema(description = "푸시 토큰", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
        String token,

        @NotNull(message = "디바이스 플랫폼은 필수입니다.")
        @Schema(description = "디바이스 플랫폼", example = "ANDROID")
        DevicePlatform platform
) {
}
