package withoutc.chongchong.study.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

public record StudyMemberJoinResponse(
        @Schema(description = "참여한 스터디 ID", example = "1")
        Long studyId
) {

    public static StudyMemberJoinResponse from(StudyMember studyMember) {
        Study study = studyMember.getStudy();
        return new StudyMemberJoinResponse(study.getId());
    }
}
