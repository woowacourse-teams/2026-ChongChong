package withoutc.chongchong.assignment.policy;

import org.springframework.stereotype.Component;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.StudyMember;

@Component
public class AssignmentAccessPolicy {

    public void requireCanCreateAssignment(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanUpdateAssignment(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanDeleteAssignment(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanReadAssignmentSubmissionStatus(StudyMember actor) {
        requireLeader(actor);
    }

    public void requireCanReadSubmissionList(Assignment assignment, StudyMember actor) {
        if (assignment.visibleToAllStudyMembers()) {
            return;
        }
        requireLeader(actor);
    }

    public void requireCanUpdateSubmission(StudyMember actor, AssignmentSubmission submission) {
        if (!submission.isOwnedBy(actor)) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }

    public void requireCanReadSubmission(StudyMember actor, Assignment assignment, AssignmentSubmission submission) {
        if (assignment.visibleToAllStudyMembers() && submission.getSubmittedAt() != null) {
            return;
        }
        if (!actor.isLeader() && !submission.isOwnedBy(actor)) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }

    private void requireLeader(StudyMember actor) {
        if (!actor.isLeader()) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }
}
