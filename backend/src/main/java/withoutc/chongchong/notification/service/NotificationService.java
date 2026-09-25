package withoutc.chongchong.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentReminder;
import withoutc.chongchong.assignment.entity.AssignmentReminderStatus;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.AssignmentReminderRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.entity.NoticeReminderStatus;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
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
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;

    private final NoticeRecipientRepository noticeRecipientRepository;
    private final NoticeReminderRepository noticeReminderRepository;
    private final NoticeRepository noticeRepository;

    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssignmentReminderRepository assignmentReminderRepository;
    private final AssignmentRepository assignmentRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final Clock clock;

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
        saveNotificationsAndPublishEvent(notice.getStudy(), recipients, NotificationType.NEW, notice.getId(),
                ResourceType.NOTICE);
    }

    @Transactional
    public void createAssignmentCreatedEventNotifications(Assignment assignment, List<StudyMember> recipients) {
        saveNotificationsAndPublishEvent(assignment.getStudy(), recipients, NotificationType.NEW, assignment.getId(),
                ResourceType.ASSIGNMENT);
    }

    @Transactional
    public void createAssignmentSubmissionSubmittedEventNotifications(AssignmentSubmission submission,
                                                                      List<StudyMember> recipients) {
        saveNotificationsAndPublishEvent(submission.getAssignment().getStudy(), recipients, NotificationType.NEW,
                submission.getId(), ResourceType.ASSIGNMENT_SUBMISSION);
    }

    @Transactional
    public void createScheduledRemindNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);

        createNoticeRemindNotifications(now);
        createAssignmentRemindNotifications(now);
    }

    private void createNoticeRemindNotifications(LocalDateTime now) {
        List<NoticeReminder> noticeReminders = noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, now);
        for (NoticeReminder noticeReminder : noticeReminders) {
            Notice notice = noticeReminder.getNotice();
            List<StudyMember> recipients = noticeRecipientRepository.findUnreadMembersByNoticeId(
                    notice.getId());
            saveNotificationsAndPublishEvent(notice.getStudy(), recipients, NotificationType.REMIND, notice.getId(),
                    ResourceType.NOTICE);
            noticeReminder.markAsSent();
        }
    }

    private void createAssignmentRemindNotifications(LocalDateTime now) {
        List<AssignmentReminder> assignmentReminders = assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, now);
        for (AssignmentReminder assignmentReminder : assignmentReminders) {
            Assignment assignment = assignmentReminder.getAssignment();
            List<StudyMember> recipients = assignmentSubmissionRepository.findUnsubmittedMembersByAssignmentId(
                    assignment.getId());
            saveNotificationsAndPublishEvent(assignment.getStudy(), recipients, NotificationType.REMIND,
                    assignment.getId(),
                    ResourceType.ASSIGNMENT);
            assignmentReminder.markAsSent();
        }
    }

    private void saveNotificationsAndPublishEvent(Study study, List<StudyMember> recipients, NotificationType type,
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
        NotificationEvent notificationEvent = NotificationEvent.create(title, body, type, resourceId, resourceType,
                deepLink, recipients);
        eventPublisher.publishEvent(notificationEvent);
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
            case NEW -> "[%s] 새 %s".formatted(studyName, resourceType.name);
            case REMIND -> "[%s] %s 리마인드".formatted(studyName, resourceType.name);
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
