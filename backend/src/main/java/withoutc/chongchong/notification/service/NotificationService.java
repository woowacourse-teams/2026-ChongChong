package withoutc.chongchong.notification.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.AssignmentReminderRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeReminderRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.controller.dto.MyNotificationListResponse;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final String COMMENT_TITLE_FORMAT = "[%s] 새 댓글";
    private static final String COMMENT_BODY_FORMAT = "%s 스터디원이 댓글을 달았어요";
    private static final String COMMENT_FOCUS_QUERY = "?focusCommentId=";

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;

    private final NoticeReminderRepository noticeReminderRepository;
    private final NoticeRepository noticeRepository;

    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssignmentReminderRepository assignmentReminderRepository;
    private final AssignmentRepository assignmentRepository;

    public MyNotificationListResponse getMyNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId);

        return MyNotificationListResponse.from(notifications);
    }

    @Transactional
    public void readNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.getByIdAndRecipientIdOrElseThrow(notificationId, userId);

        notification.read();
    }

    @Transactional
    public void createNoticeCreatedEventNotifications(Notice notice, List<StudyMember> recipients) {
        saveNotifications(notice.getStudy(), recipients, NotificationType.NEW, notice.getId(),
                ResourceType.NOTICE);
    }

    @Transactional
    public void createAssignmentCreatedEventNotifications(Assignment assignment, List<StudyMember> recipients,
                                                          Long userId) {

        recipients = recipients.stream()
                .filter(recipient -> !recipient.getUser().getId().equals(userId))
                .toList();
        saveNotifications(assignment.getStudy(), recipients, NotificationType.NEW, assignment.getId(),
                ResourceType.ASSIGNMENT);
    }

    @Transactional
    public void createAssignmentSubmissionSubmittedEventNotifications(AssignmentSubmission submission,
                                                                      List<StudyMember> recipients) {
        saveNotifications(submission.getAssignment().getStudy(), recipients, NotificationType.NEW,
                submission.getId(), ResourceType.ASSIGNMENT_SUBMISSION);
    }

    @Transactional
    public void createCommentCreatedEventNotifications(Comment comment, Study study, StudyMember author,
                                                        List<StudyMember> recipients) {
        String title = COMMENT_TITLE_FORMAT.formatted(study.getName());
        String body = COMMENT_BODY_FORMAT.formatted(author.getName());
        String deepLink = createDeepLink(study.getId(), comment.getResourceType(), comment.getResourceId())
                + COMMENT_FOCUS_QUERY + comment.getId();
        Set<Long> notifiedUsers = new HashSet<>();
        for (StudyMember recipient : recipients) {
            Long recipientId = recipient.getUser().getId();
            if (recipientId.equals(author.getUser().getId()) || !notifiedUsers.add(recipientId)) {
                continue;
            }
            Notification notification = Notification.createForComment(comment, recipient, title, body, deepLink);
            notificationRepository.save(notification);
            saveNotificationDelivery(notification);
        }
    }

    @Transactional
    public void createReminderEventNotifications(Study study, List<StudyMember> recipients,
                                                  Long resourceId, ResourceType resourceType) {
        saveNotifications(study, recipients, NotificationType.REMIND, resourceId, resourceType);
    }

    @Transactional
    public void deleteNotificationsForStudy(Long studyId) {
        noticeReminderRepository.findAllByStudyIdForUpdate(studyId);
        assignmentReminderRepository.findAllByStudyIdForUpdate(studyId);
        notificationRepository.deleteAllByStudyId(studyId);
    }

    @Transactional
    public void deleteNotificationsForResource(ResourceType resourceType, Long resourceId) {
        // 리마인드 알림 생성과 삭제가 겹치지 않도록 리마인더 행 잠금
        if (resourceType == ResourceType.NOTICE) {
            noticeReminderRepository.findAllByNoticeIdForUpdate(resourceId);
        }
        if (resourceType == ResourceType.ASSIGNMENT) {
            assignmentReminderRepository.findAllByAssignmentIdForUpdate(resourceId);
        }

        notificationRepository.deleteAllByResourceTypeAndResourceId(resourceType, resourceId);
        if (resourceType == ResourceType.ASSIGNMENT) {
            notificationRepository.deleteAllForAssignmentSubmissions(resourceId);
        }
    }

    @Transactional
    public void deleteNotificationsForStudyMember(Long memberId) {
        notificationRepository.deleteAllByMemberId(memberId);
    }

    @Transactional
    public void deleteNotificationsForLeaderSubmissions(Long assignmentId) {
        notificationRepository.deleteAllForLeaderSubmissions(assignmentId);
    }

    @Transactional
    public void deleteNotificationsForUser(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    private void saveNotifications(Study study, List<StudyMember> recipients, NotificationType type,
                                   Long resourceId,
                                   ResourceType resourceType) {
        String title = createTitle(study.getName(), type, resourceType);
        String body = createBody(resourceId, resourceType);
        String deepLink = createDeepLink(study.getId(), resourceType, resourceId);
        for (StudyMember recipient : recipients) {
            Notification notification = Notification.create(recipient.getUser(), title, body, type, resourceId,
                    resourceType, deepLink);
            notificationRepository.save(notification);
            saveNotificationDelivery(notification);
        }
    }

    private void saveNotificationDelivery(Notification notification) {
        List<WebPushSubscription> subscriptions = webPushSubscriptionRepository.findByUserIdAndIsActiveTrue(
                notification.getRecipient().getId());
        for (WebPushSubscription subscription : subscriptions) {
            NotificationDelivery delivery = NotificationDelivery.create(notification, subscription);
            notificationDeliveryRepository.save(delivery);
        }
    }

    private String createTitle(String studyName, NotificationType type, ResourceType resourceType) {
        return switch (type) {
            case NEW -> "[%s] 새 %s".formatted(studyName, resourceType.getName());
            case REMIND -> "[%s] %s 리마인드".formatted(studyName, resourceType.getName());
        };
    }

    private String createBody(Long resourceId, ResourceType resourceType) {
        return switch (resourceType) {
            case NOTICE -> noticeRepository.getByIdOrThrow(resourceId).getTitle();
            case ASSIGNMENT -> assignmentRepository.getByIdOrThrow(resourceId).getTitle();
            // 알림 저장은 User로 하되, 알림에 표시되는 이름은 StudyMember 이름으로 함
            case ASSIGNMENT_SUBMISSION -> "%s 스터디원이 과제를 제출했어요".formatted(
                    assignmentSubmissionRepository.getByIdOrThrow(resourceId).getMember().getName());
        };
    }

    private String createDeepLink(Long studyId, ResourceType resourceType, Long resourceId) {
        return switch (resourceType) {
            case NOTICE -> "/studies/%d/notices/%d".formatted(studyId, resourceId);
            case ASSIGNMENT -> "/studies/%d/assignments/%d".formatted(studyId, resourceId);
            case ASSIGNMENT_SUBMISSION -> "/studies/%d/assignments/%d/submissions/%d".formatted(studyId,
                    assignmentSubmissionRepository.getByIdOrThrow(resourceId).getAssignment().getId(), resourceId);
        };
    }
}
