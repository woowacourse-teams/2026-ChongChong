package withoutc.chongchong.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record StudyInviteTokenRequest(
        @NotBlank(message = "초대 토큰은 필수입니다.")
        @Schema(description = "스터디 초대 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token
) {
}
