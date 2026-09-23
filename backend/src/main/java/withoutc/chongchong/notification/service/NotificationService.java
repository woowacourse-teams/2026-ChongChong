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
    public void createNoticeCreatedEventNotifications(Notice notice, List<StudyMember> recipients) {
        saveNoticeNotifications(notice, recipients, NotificationType.CREATED);

        NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.CREATED, notice.getId(),
                ResourceType.NOTICE,
                notice.getStudy().getId(), notice.getTitle(), recipients);
        eventPublisher.publishEvent(notificationEvent);
    }

    @Transactional
    public void createAssignmentCreatedEventNotifications(Assignment assignment, List<StudyMember> recipients) {
        saveAssignmentNotifications(assignment, recipients, NotificationType.CREATED);

        NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.CREATED, assignment.getId(),
                ResourceType.ASSIGNMENT,
                assignment.getStudy().getId(), assignment.getTitle(), recipients);
        eventPublisher.publishEvent(notificationEvent);
    }

    @Transactional
    public void createAssignmentSubmissionSubmittedEventNotifications(AssignmentSubmission submission,
                                                                      List<StudyMember> recipients) {
        saveAssignmentSubmissionNotifications(submission, recipients);

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
            saveNoticeNotifications(notice, recipients, NotificationType.REMIND);
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
            saveAssignmentNotifications(assignment, recipients, NotificationType.REMIND);
            assignmentReminder.markAsSent();
            NotificationEvent notificationEvent = NotificationEvent.create(NotificationType.REMIND, assignment.getId(),
                    ResourceType.ASSIGNMENT,
                    assignment.getStudy().getId(), assignment.getTitle(), recipients);
            eventPublisher.publishEvent(notificationEvent);
        }
    }

    private void saveNoticeNotifications(Notice notice, List<StudyMember> recipients, NotificationType type) {
        Study study = notice.getStudy();
        saveNotifications(
                recipients,
                type,
                notice.getId(),
                ResourceType.NOTICE,
                createTitle(study, ResourceType.NOTICE),
                notice.getTitle(),
                "/studies/%d/notices/%d".formatted(study.getId(), notice.getId())
        );
    }

    private void saveAssignmentNotifications(Assignment assignment, List<StudyMember> recipients,
                                             NotificationType type) {
        Study study = assignment.getStudy();
        saveNotifications(
                recipients,
                type,
                assignment.getId(),
                ResourceType.ASSIGNMENT,
                createTitle(study, ResourceType.ASSIGNMENT),
                assignment.getTitle(),
                "/studies/%d/assignments/%d".formatted(study.getId(), assignment.getId())
        );
    }

    private void saveAssignmentSubmissionNotifications(AssignmentSubmission submission,
                                                       List<StudyMember> recipients) {
        Assignment assignment = submission.getAssignment();
        Study study = assignment.getStudy();
        saveNotifications(
                recipients,
                NotificationType.SUBMITTED,
                submission.getId(),
                ResourceType.ASSIGNMENT_SUBMISSION,
                createTitle(study, ResourceType.ASSIGNMENT_SUBMISSION),
                "%s 스터디원이 과제를 제출했어요".formatted(submission.getMember().getName()),
                "/studies/%d/assignments/%d/submissions/%d".formatted(
                        study.getId(), assignment.getId(), submission.getId())
        );
    }

    private String createTitle(Study study, ResourceType resourceType) {
        return "[%s] 새 %s".formatted(study.getName(), resourceType.name);
    }

    private void saveNotifications(
            List<StudyMember> recipients,
            NotificationType type,
            Long resourceId,
            ResourceType resourceType,
            String title,
            String body,
            String deepLink
    ) {
        for (StudyMember recipient : recipients) {
            Notification notification = Notification.create(
                    recipient.getUser(), title, body, type, resourceId, resourceType, deepLink);
            notificationRepository.save(notification);
        }
    }
}
