package withoutc.chongchong.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.comment.exception.CommentException;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Component
@RequiredArgsConstructor
public class CommentTargetResolver {

    private final NoticeRepository noticeRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentAccessPolicy assignmentAccessPolicy;

    ResolvedTarget resolve(Long studyId, CommentTarget target, boolean forUpdate) {
        return switch (target.resourceType()) {
            case NOTICE -> {
                Notice notice;
                if (forUpdate) {
                    notice = noticeRepository.getByIdAndStudyIdForUpdateOrThrow(target.resourceId(), studyId);
                } else {
                    notice = noticeRepository.getByIdAndStudyIdOrThrow(target.resourceId(), studyId);
                }
                yield new ResolvedTarget(notice.getStudy(), null);
            }
            case ASSIGNMENT -> new ResolvedTarget(assignment(studyId, target.resourceId(), forUpdate).getStudy(), null);
            case ASSIGNMENT_SUBMISSION -> {
                Assignment assignment = assignment(studyId, target.assignmentId(), forUpdate);
                AssignmentSubmission submission;
                if (forUpdate) {
                    submission = submissionRepository.getByIdAndAssignmentIdForUpdateOrThrow(
                            target.resourceId(), assignment.getId());
                } else {
                    submission = submissionRepository.getByIdAndAssignmentIdOrThrow(
                            target.resourceId(), assignment.getId());
                }
                yield new ResolvedTarget(assignment.getStudy(), submission);
            }
        };
    }

    void requireCanRead(StudyMember actor, ResolvedTarget target) {
        if (target.submission() != null) {
            assignmentAccessPolicy.requireCanReadSubmission(actor, target.submission().getAssignment(),
                    target.submission());
        }
    }

    void requireCanCreate(StudyMember actor, ResolvedTarget target) {
        requireCanRead(actor, target);
        if (target.submission() != null && target.submission().getSubmittedAt() == null) {
            throw new CommentException(CommentErrorCode.TARGET_NOT_READY);
        }
    }

    private Assignment assignment(Long studyId, Long assignmentId, boolean forUpdate) {
        if (forUpdate) {
            return assignmentRepository.getByIdAndStudyIdForUpdateOrThrow(assignmentId, studyId);
        }
        return assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);
    }

    record ResolvedTarget(Study study, AssignmentSubmission submission) {
    }
}
