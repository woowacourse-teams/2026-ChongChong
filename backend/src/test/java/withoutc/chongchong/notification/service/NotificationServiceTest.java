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
import org.springframework.context.ApplicationEventPublisher;
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
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.user.entity.User;

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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                noticeRecipientRepository,
                noticeReminderRepository,
                assignmentSubmissionRepository,
                assignmentReminderRepository,
                eventPublisher,
                CLOCK
        );
    }

    @Test
    @DisplayName("기한이 지난 과제 리마인더는 미제출자에게 알림을 생성하고 발송 완료 처리한다")
    void createAssignmentReminderNotifications() {
        AssignmentReminder reminder = mock(AssignmentReminder.class);
        Assignment assignment = mock(Assignment.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        when(assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, NOW)).thenReturn(List.of(reminder));
        when(noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, NOW)).thenReturn(List.of());
        when(reminder.getAssignment()).thenReturn(assignment);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(assignment.getStudy()).thenReturn(study);
        when(assignment.getTitle()).thenReturn("과제 제목");
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(recipient.getUser()).thenReturn(recipientUser);
        when(assignmentSubmissionRepository.findUnsubmittedMembersByAssignmentId(ASSIGNMENT_ID))
                .thenReturn(List.of(recipient));

        notificationService.createScheduledRemindNotifications();

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isSameAs(recipientUser);
        assertThat(notification.getTitle()).isEqualTo("[스터디] 새 과제");
        assertThat(notification.getBody()).isEqualTo("과제 제목");
        assertThat(notification.getType()).isEqualTo(NotificationType.REMIND);
        assertThat(notification.getResourceId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.ASSIGNMENT);
        assertThat(notification.getDeepLink()).isEqualTo("/studies/1/assignments/200");
        assertThat(notification.isRead()).isFalse();
        verify(reminder).markAsSent();
        verify(assignmentSubmissionRepository).findUnsubmittedMembersByAssignmentId(ASSIGNMENT_ID);
        verifyNoInteractions(noticeRecipientRepository);
    }

    @Test
    @DisplayName("기한이 지난 공지 리마인더는 읽지 않은 수신자에게 알림을 생성하고 발송 완료 처리한다")
    void createNoticeReminderNotifications() {
        NoticeReminder reminder = mock(NoticeReminder.class);
        Notice notice = mock(Notice.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        when(assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, NOW)).thenReturn(List.of());
        when(noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, NOW)).thenReturn(List.of(reminder));
        when(reminder.getNotice()).thenReturn(notice);
        when(notice.getId()).thenReturn(NOTICE_ID);
        when(notice.getStudy()).thenReturn(study);
        when(notice.getTitle()).thenReturn("공지 제목");
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(recipient.getUser()).thenReturn(recipientUser);
        when(noticeRecipientRepository.findUnreadMembersByNoticeId(NOTICE_ID))
                .thenReturn(List.of(recipient));

        notificationService.createScheduledRemindNotifications();

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isSameAs(recipientUser);
        assertThat(notification.getTitle()).isEqualTo("[스터디] 새 공지");
        assertThat(notification.getBody()).isEqualTo("공지 제목");
        assertThat(notification.getType()).isEqualTo(NotificationType.REMIND);
        assertThat(notification.getResourceId()).isEqualTo(NOTICE_ID);
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.NOTICE);
        assertThat(notification.getDeepLink()).isEqualTo("/studies/1/notices/100");
        verify(reminder).markAsSent();
        verify(noticeRecipientRepository).findUnreadMembersByNoticeId(NOTICE_ID);
        verifyNoInteractions(assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("과제 제출 이벤트 알림은 제출 리소스와 수신자를 저장한다")
    void createAssignmentSubmissionSubmittedEventNotifications() {
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        Assignment assignment = mock(Assignment.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);

        when(submission.getAssignment()).thenReturn(assignment);
        when(submission.getId()).thenReturn(300L);
        when(assignment.getStudy()).thenReturn(study);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        StudyMember submitter = mock(StudyMember.class);
        when(submission.getMember()).thenReturn(submitter);
        when(submitter.getName()).thenReturn("제출자");
        when(recipient.getUser()).thenReturn(recipientUser);

        notificationService.createAssignmentSubmissionSubmittedEventNotifications(submission, List.of(recipient));

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isSameAs(recipientUser);
        assertThat(notification.getTitle()).isEqualTo("[스터디] 새 제출물");
        assertThat(notification.getBody()).isEqualTo("제출자 스터디원이 과제를 제출했어요");
        assertThat(notification.getType()).isEqualTo(NotificationType.SUBMITTED);
        assertThat(notification.getResourceId()).isEqualTo(300L);
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.ASSIGNMENT_SUBMISSION);
        assertThat(notification.getDeepLink()).isEqualTo("/studies/1/assignments/200/submissions/300");
        assertThat(notification.isRead()).isFalse();

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        NotificationEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(NotificationType.SUBMITTED);
        assertThat(event.resourceId()).isEqualTo(300L);
        assertThat(event.resourceType()).isEqualTo(NotificationResourceType.ASSIGNMENT_SUBMISSION);
    }

    @Test
    @DisplayName("대상이 되는 리마인더가 없으면 알림을 생성하지 않는다")
    void doNothingWhenNoPendingDueReminder() {
        when(assignmentReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                AssignmentReminderStatus.PENDING, NOW)).thenReturn(List.of());
        when(noticeReminderRepository.findAllByStatusAndRemindAtLessThanEqual(
                NoticeReminderStatus.PENDING, NOW)).thenReturn(List.of());

        notificationService.createScheduledRemindNotifications();

        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(noticeRecipientRepository, assignmentSubmissionRepository);
    }
}
