package withoutc.chongchong.study.controller.dto;

import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

public record StudyMemberJoinResponse(
        Long studyId
) {

    public static StudyMemberJoinResponse from(StudyMember studyMember) {
        Study study = studyMember.getStudy();
        return new StudyMemberJoinResponse(study.getId());
    }
}
