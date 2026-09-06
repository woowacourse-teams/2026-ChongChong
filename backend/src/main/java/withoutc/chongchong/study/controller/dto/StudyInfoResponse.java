package withoutc.chongchong.study.controller.dto;

import withoutc.chongchong.study.entity.StudyMemberRole;

public record StudyInfoResponse(
        String studyName,
        StudyMemberRole role,
        String userName
) {
}
