package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import withoutc.chongchong.study.entity.StudyMember;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long NOTICE_ID = 100L;
    private static final Long ASSIGNMENT_ID = 200L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"),
            ZoneId.of("Asia/Seoul"));

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NoticeRecipientRepository noticeRecipientRepository;

    @Mock
    private NoticeReminderRepository noticeReminderRepository;

    @Mock
    private AssignmentReminderRepository assignmentReminderRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                noticeRecipientRepository,
                noticeReminderRepository,
                assignmentReminderRepository,
                assignmentSubmissionRepository,
                CLOCK
        );
    }

    @Test
    @DisplayName("기한이 지난 과제 리마인더는 미제출자에게 알림을 생성하고 발송 완료 처리한다")
    void createAssignmentReminderNotifications() {
        AssignmentReminder reminder = mock(AssignmentReminder.class);
        Assignment assignment = mock(Assignment.class);
        Study study = mock(Study.class);
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        StudyMember recipient = mock(StudyMember.class);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        when(assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, NOW)).thenReturn(List.of(reminder));
        when(noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, NOW)).thenReturn(List.of());
        when(reminder.getAssignment()).thenReturn(assignment);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(assignment.getStudy()).thenReturn(study);
        when(assignmentSubmissionRepository.findAllByAssignmentIdAndSubmittedAtIsNull(ASSIGNMENT_ID))
                .thenReturn(List.of(submission));
        when(submission.getMember()).thenReturn(recipient);

        notificationService.createNotification();

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getStudy()).isSameAs(study);
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getType()).isEqualTo(NotificationType.REMIND);
        assertThat(notification.getResourceId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.ASSIGNMENT);
        assertThat(notification.isRead()).isFalse();
        verify(reminder).markAsSent();
        verify(assignmentSubmissionRepository).findAllByAssignmentIdAndSubmittedAtIsNull(ASSIGNMENT_ID);
        verifyNoInteractions(noticeRecipientRepository);
    }

    @Test
    @DisplayName("기한이 지난 공지 리마인더는 읽지 않은 수신자에게 알림을 생성하고 발송 완료 처리한다")
    void createNoticeReminderNotifications() {
        NoticeReminder reminder = mock(NoticeReminder.class);
        Notice notice = mock(Notice.class);
        Study study = mock(Study.class);
        NoticeRecipient noticeRecipient = mock(NoticeRecipient.class);
        StudyMember recipient = mock(StudyMember.class);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        when(assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, NOW)).thenReturn(List.of());
        when(noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, NOW)).thenReturn(List.of(reminder));
        when(reminder.getNotice()).thenReturn(notice);
        when(notice.getId()).thenReturn(NOTICE_ID);
        when(notice.getStudy()).thenReturn(study);
        when(noticeRecipientRepository.findAllByNoticeIdAndReadAtIsNull(NOTICE_ID))
                .thenReturn(List.of(noticeRecipient));
        when(noticeRecipient.getMember()).thenReturn(recipient);

        notificationService.createNotification();

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getStudy()).isSameAs(study);
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getType()).isEqualTo(NotificationType.REMIND);
        assertThat(notification.getResourceId()).isEqualTo(NOTICE_ID);
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.NOTICE);
        verify(reminder).markAsSent();
        verify(noticeRecipientRepository).findAllByNoticeIdAndReadAtIsNull(NOTICE_ID);
        verifyNoInteractions(assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("대상이 되는 리마인더가 없으면 알림을 생성하지 않는다")
    void doNothingWhenNoPendingDueReminder() {
        when(assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, NOW)).thenReturn(List.of());
        when(noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, NOW)).thenReturn(List.of());

        notificationService.createNotification();

        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(noticeRecipientRepository, assignmentSubmissionRepository);
    }
}
