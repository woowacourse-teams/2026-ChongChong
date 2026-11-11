package withoutc.chongchong.comment.service;

import withoutc.chongchong.notification.entity.ResourceType;

public record CommentTarget(ResourceType resourceType, Long resourceId, Long assignmentId) {

    public static CommentTarget notice(Long noticeId) {
        return new CommentTarget(ResourceType.NOTICE, noticeId, null);
    }

    public static CommentTarget assignment(Long assignmentId) {
        return new CommentTarget(ResourceType.ASSIGNMENT, assignmentId, null);
    }

    public static CommentTarget submission(Long assignmentId, Long submissionId) {
        return new CommentTarget(ResourceType.ASSIGNMENT_SUBMISSION, submissionId, assignmentId);
    }
}
