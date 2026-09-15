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
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.entity.NoticeReminderStatus;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeReminderRepository;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.study.entity.Study;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;
    private final NoticeReminderRepository noticeReminderRepository;
    private final AssignmentReminderRepository assignmentReminderRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final Clock clock;

    @Transactional
    public void createNotification() {
        LocalDateTime now = LocalDateTime.now(clock);

        createAssignmentRemindNotification(now);
        createNoticeRemindNotification(now);
    }

    private void createNoticeRemindNotification(LocalDateTime now) {
        List<NoticeReminder> noticeReminders = noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, now);
        for (NoticeReminder noticeReminder : noticeReminders) {
            Notice notice = noticeReminder.getNotice();
            Study study = notice.getStudy();
            List<NoticeRecipient> recipients = noticeRecipientRepository.findAllByNoticeIdAndReadAtIsNull(
                    notice.getId());
            for (NoticeRecipient recipient : recipients) {
                Notification notification = Notification.create(study, recipient.getMember(), NotificationType.REMIND,
                        notice.getId(), NotificationResourceType.NOTICE);
                notificationRepository.save(notification);
            }
            noticeReminder.markAsSent();
        }
    }

    private void createAssignmentRemindNotification(LocalDateTime now) {
        List<AssignmentReminder> assignmentReminders = assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, now);
        for (AssignmentReminder assignmentReminder : assignmentReminders) {
            Assignment assignment = assignmentReminder.getAssignment();
            Study study = assignment.getStudy();
            List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findAllByAssignmentIdAndSubmittedAtIsNull(
                    assignment.getId());
            for (AssignmentSubmission submission : submissions) {
                Notification notification = Notification.create(study, submission.getMember(), NotificationType.REMIND,
                        assignment.getId(), NotificationResourceType.ASSIGNMENT);
                notificationRepository.save(notification);
            }
            assignmentReminder.markAsSent();
        }
    }
}
