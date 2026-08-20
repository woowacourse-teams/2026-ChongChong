package withoutc.chongchong.study.dto;

import jakarta.validation.constraints.NotBlank;

public record StudyInviteTokenRequest(
        @NotBlank(message = "초대 토큰은 필수입니다.")
        String token
) {
}
