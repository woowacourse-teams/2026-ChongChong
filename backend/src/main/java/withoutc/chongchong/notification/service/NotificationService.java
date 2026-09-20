package withoutc.chongchong.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.repository.NotificationRepository;
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
    private final Clock clock;

    @Transactional
    public void createNoticeCreatedEventNotifications(Notice notice, List<StudyMember> recipients) {
        saveNotifications(notice.getStudy(), recipients, NotificationType.CREATED, notice.getId(),
                NotificationResourceType.NOTICE);
    }

    @Transactional
    public void createAssignmentCreatedEventNotifications(Assignment assignment, List<StudyMember> recipients) {
        saveNotifications(assignment.getStudy(), recipients, NotificationType.CREATED, assignment.getId(),
                NotificationResourceType.ASSIGNMENT);
    }

    @Transactional
    public void createAssignmentSubmissionSubmittedEventNotifications(AssignmentSubmission submission,
                                                                      List<StudyMember> recipients) {
        saveNotifications(submission.getAssignment().getStudy(), recipients, NotificationType.SUBMITTED,
                submission.getId(), NotificationResourceType.ASSIGNMENT_SUBMISSION);
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
            Study study = notice.getStudy();
            List<StudyMember> recipients = noticeRecipientRepository.findUnreadMembersByNoticeId(
                    notice.getId());
            saveNotifications(study, recipients, NotificationType.REMIND, notice.getId(),
                    NotificationResourceType.NOTICE);
            noticeReminder.markAsSent();
        }
    }

    private void createAssignmentRemindNotifications(LocalDateTime now) {
        List<AssignmentReminder> assignmentReminders = assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, now);
        for (AssignmentReminder assignmentReminder : assignmentReminders) {
            Assignment assignment = assignmentReminder.getAssignment();
            Study study = assignment.getStudy();
            List<StudyMember> recipients = assignmentSubmissionRepository.findUnsubmittedMembersByAssignmentId(
                    assignment.getId());
            saveNotifications(study, recipients, NotificationType.REMIND, assignment.getId(),
                    NotificationResourceType.ASSIGNMENT);
            assignmentReminder.markAsSent();
        }
    }

    private void saveNotifications(Study study, List<StudyMember> recipients, NotificationType type, Long resourceId,
                                   NotificationResourceType resourceType) {
        for (StudyMember recipient : recipients) {
            Notification notification = Notification.create(study, recipient, type, resourceId, resourceType);
            notificationRepository.save(notification);
        }
    }
}
