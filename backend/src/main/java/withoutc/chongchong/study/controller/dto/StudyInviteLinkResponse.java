package withoutc.chongchong.study.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudyInviteLinkResponse(
        @Schema(description = "스터디 초대 링크", example = "https://chongchong.app/studies/join?token=eyJ...")
        String inviteLink
) {
}
