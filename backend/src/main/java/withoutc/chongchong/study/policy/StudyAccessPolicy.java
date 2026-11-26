package withoutc.chongchong.study.policy;

import org.springframework.stereotype.Component;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;

@Component
public class StudyAccessPolicy {

    public void requireCanUpdateStudy(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanDeleteStudy(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanExpelMember(StudyMember actor) {
        requireLeader(actor);
    }

    // 기존 API 응답(403 NOT_STUDY_LEADER)을 유지한다. ADR-0068
    private void requireLeader(StudyMember actor) {
        if (!actor.isLeader()) {
            throw new StudyMemberException(StudyMemberErrorCode.NOT_STUDY_LEADER);
        }
    }
}
