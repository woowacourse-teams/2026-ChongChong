package withoutc.chongchong.user.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserProfileImageUpdateRequest(
        @NotNull(message = "프로필 이미지 파일 ID는 필수입니다.")
        @Positive(message = "프로필 이미지 파일 ID는 양수여야 합니다.")
        Long fileId
) {
}
