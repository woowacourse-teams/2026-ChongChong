package withoutc.chongchong.study.dto;

import jakarta.validation.constraints.NotBlank;

public record StudyInviteTokenRequest(
        @NotBlank
        String token
) {
}
