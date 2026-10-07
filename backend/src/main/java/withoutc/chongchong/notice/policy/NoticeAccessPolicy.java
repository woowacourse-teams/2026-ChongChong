package withoutc.chongchong.notice.policy;

import org.springframework.stereotype.Component;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.StudyMember;

@Component
public class NoticeAccessPolicy {

    public void requireCanCreateNotice(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanUpdateNotice(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanDeleteNotice(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanReadNoticeReadStatuses(StudyMember actor) {
        requireLeader(actor);
    }

    private void requireLeader(StudyMember actor) {
        if (!actor.isLeader()) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }
}
