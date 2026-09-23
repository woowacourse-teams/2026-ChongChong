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
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.entity.NoticeReminderStatus;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeReminderRepository;
import withoutc.chongchong.notification.controller.dto.MyNotificationListResponse;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;
    private final NoticeReminderRepository noticeReminderRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssignmentReminderRepository assignmentReminderRepository;

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
        saveNotifications(notice.getStudy(), recipients, NotificationType.CREATED, notice.getId(),
                ResourceType.NOTICE);

        NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.CREATED, notice.getId(),
                ResourceType.NOTICE,
                notice.getStudy().getId(), notice.getTitle(), recipients);
        eventPublisher.publishEvent(notificationEvent);
    }

    @Transactional
    public void createAssignmentCreatedEventNotifications(Assignment assignment, List<StudyMember> recipients) {
        saveNotifications(assignment.getStudy(), recipients, NotificationType.CREATED, assignment.getId(),
                ResourceType.ASSIGNMENT);

        NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.CREATED, assignment.getId(),
                ResourceType.ASSIGNMENT,
                assignment.getStudy().getId(), assignment.getTitle(), recipients);
        eventPublisher.publishEvent(notificationEvent);
    }

    @Transactional
    public void createAssignmentSubmissionSubmittedEventNotifications(AssignmentSubmission submission,
                                                                      List<StudyMember> recipients) {
        saveNotifications(submission.getAssignment().getStudy(), recipients, NotificationType.SUBMITTED,
                submission.getId(), ResourceType.ASSIGNMENT_SUBMISSION);

        NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.SUBMITTED, submission.getId(),
                ResourceType.ASSIGNMENT_SUBMISSION,
                submission.getAssignment().getStudy().getId(), submission.getContent(), recipients);
        eventPublisher.publishEvent(notificationEvent);
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
            saveNotifications(notice.getStudy(), recipients, NotificationType.REMIND, notice.getId(),
                    ResourceType.NOTICE);
            noticeReminder.markAsSent();
            NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.REMIND, notice.getId(),
                    ResourceType.NOTICE,
                    notice.getStudy().getId(), notice.getTitle(), recipients);
            eventPublisher.publishEvent(notificationEvent);
        }
    }

    private void createAssignmentRemindNotifications(LocalDateTime now) {
        List<AssignmentReminder> assignmentReminders = assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, now);
        for (AssignmentReminder assignmentReminder : assignmentReminders) {
            Assignment assignment = assignmentReminder.getAssignment();
            List<StudyMember> recipients = assignmentSubmissionRepository.findUnsubmittedMembersByAssignmentId(
                    assignment.getId());
            saveNotifications(assignment.getStudy(), recipients, NotificationType.REMIND, assignment.getId(),
                    ResourceType.ASSIGNMENT);
            assignmentReminder.markAsSent();
            NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.REMIND, assignment.getId(),
                    ResourceType.ASSIGNMENT,
                    assignment.getStudy().getId(), assignment.getTitle(), recipients);
            eventPublisher.publishEvent(notificationEvent);
        }
    }

    // TODO: 자동 알림 구현 후 Notification 저장을 다시 활성화한다.
    private void saveNotifications(Study study, List<StudyMember> recipients, NotificationType type, Long resourceId,
                                   ResourceType resourceType) {
    }
}
