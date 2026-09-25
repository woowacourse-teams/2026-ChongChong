package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.AssignmentReminderRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeReminderRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
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
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long NOTICE_ID = 100L;
    private static final Long ASSIGNMENT_ID = 200L;
    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 300L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"),
            ZoneId.of("Asia/Seoul"));

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Mock
    private NoticeRecipientRepository noticeRecipientRepository;

    @Mock
    private NoticeReminderRepository noticeReminderRepository;

    @Mock
    private AssignmentReminderRepository assignmentReminderRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private NoticeRepository noticeRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationDeliveryRepository,
                webPushSubscriptionRepository,
                noticeRecipientRepository,
                noticeReminderRepository,
                noticeRepository,
                assignmentSubmissionRepository,
                assignmentReminderRepository,
                assignmentRepository,
                CLOCK
        );
    }

    @Test
    @DisplayName("현재 사용자의 알림을 읽음 처리한다")
    void readNotification() {
        User recipient = mock(User.class);
        Notification notification = Notification.create(
                recipient,
                "[스터디] 새 공지",
                "공지 내용",
                NotificationType.NEW,
                NOTICE_ID,
                ResourceType.NOTICE,
                "/studies/1/notices/100"
        );
        when(notificationRepository.getByIdAndRecipientIdOrElseThrow(NOTIFICATION_ID, USER_ID))
                .thenReturn(notification);

        notificationService.readNotification(USER_ID, NOTIFICATION_ID);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).getByIdAndRecipientIdOrElseThrow(NOTIFICATION_ID, USER_ID);
    }

    @Test
    @DisplayName("기한이 지난 과제 리마인더는 미제출자를 조회하고 발송 완료 처리한다")
    void createAssignmentReminderNotifications() {
        AssignmentReminder reminder = mock(AssignmentReminder.class);
        Assignment assignment = mock(Assignment.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        WebPushSubscription subscription = mock(WebPushSubscription.class);
        when(assignmentReminderRepository.findClaimableForUpdate(NOW, 100)).thenReturn(List.of(reminder));
        when(noticeReminderRepository.findClaimableForUpdate(NOW, 100)).thenReturn(List.of());
        when(reminder.getAssignment()).thenReturn(assignment);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(assignment.getStudy()).thenReturn(study);
        when(assignment.getTitle()).thenReturn("과제 제목");
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipientUser.getId()).thenReturn(USER_ID);
        when(webPushSubscriptionRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of(subscription));
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findUnsubmittedMembersByAssignmentId(ASSIGNMENT_ID))
                .thenReturn(List.of(recipient));

        notificationService.createScheduledRemindNotifications();

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationDeliveryRepository).save(any(NotificationDelivery.class));
        verify(reminder).markAsProcessing();
        verify(reminder).markAsSent();
        verify(assignmentSubmissionRepository).findUnsubmittedMembersByAssignmentId(ASSIGNMENT_ID);
        verifyNoInteractions(noticeRecipientRepository);
    }

    @Test
    @DisplayName("기한이 지난 공지 리마인더는 읽지 않은 수신자를 조회하고 발송 완료 처리한다")
    void createNoticeReminderNotifications() {
        NoticeReminder reminder = mock(NoticeReminder.class);
        Notice notice = mock(Notice.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        when(assignmentReminderRepository.findClaimableForUpdate(NOW, 100)).thenReturn(List.of());
        when(noticeReminderRepository.findClaimableForUpdate(NOW, 100)).thenReturn(List.of(reminder));
        when(reminder.getNotice()).thenReturn(notice);
        when(notice.getId()).thenReturn(NOTICE_ID);
        when(notice.getStudy()).thenReturn(study);
        when(notice.getTitle()).thenReturn("공지 제목");
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipientUser.getId()).thenReturn(USER_ID);
        when(webPushSubscriptionRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of());
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);
        when(noticeRecipientRepository.findUnreadMembersByNoticeId(NOTICE_ID))
                .thenReturn(List.of(recipient));

        notificationService.createScheduledRemindNotifications();

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationDeliveryRepository, never()).save(any(NotificationDelivery.class));
        verify(reminder).markAsProcessing();
        verify(reminder).markAsSent();
        verify(noticeRecipientRepository).findUnreadMembersByNoticeId(NOTICE_ID);
        verifyNoInteractions(assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("과제 제출 알림은 Notification을 저장한다")
    void createAssignmentSubmissionSubmittedEventNotifications() {
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        Assignment assignment = mock(Assignment.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        StudyMember submitter = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        when(submission.getAssignment()).thenReturn(assignment);
        when(submission.getId()).thenReturn(300L);
        when(assignment.getStudy()).thenReturn(study);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(submission.getMember()).thenReturn(submitter);
        when(submitter.getName()).thenReturn("제출자");
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipientUser.getId()).thenReturn(USER_ID);
        when(assignmentSubmissionRepository.getByIdOrThrow(300L)).thenReturn(submission);

        notificationService.createAssignmentSubmissionSubmittedEventNotifications(submission, List.of(recipient));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.NEW);
        assertThat(notification.getBody()).isEqualTo("제출자 스터디원이 과제를 제출했어요");
        assertThat(notification.getDeepLink()).isEqualTo("/studies/1/assignments/200/submissions/300");

        verify(webPushSubscriptionRepository).findByUserIdAndIsActiveTrue(USER_ID);
    }

    @Test
    @DisplayName("사용자의 활성 Web Push 구독마다 Delivery를 생성하고 Notification보다 나중에 저장한다")
    void createDeliveryForEachActiveWebPushSubscription() {
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        Assignment assignment = mock(Assignment.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        StudyMember submitter = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        WebPushSubscription desktopSubscription = mock(WebPushSubscription.class);
        WebPushSubscription mobileSubscription = mock(WebPushSubscription.class);
        when(submission.getAssignment()).thenReturn(assignment);
        when(submission.getId()).thenReturn(300L);
        when(assignment.getStudy()).thenReturn(study);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(submission.getMember()).thenReturn(submitter);
        when(submitter.getName()).thenReturn("제출자");
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipientUser.getId()).thenReturn(USER_ID);
        when(assignmentSubmissionRepository.getByIdOrThrow(300L)).thenReturn(submission);
        when(webPushSubscriptionRepository.findByUserIdAndIsActiveTrue(USER_ID))
                .thenReturn(List.of(desktopSubscription, mobileSubscription));

        notificationService.createAssignmentSubmissionSubmittedEventNotifications(submission, List.of(recipient));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepository, times(2)).save(deliveryCaptor.capture());

        assertThat(deliveryCaptor.getAllValues())
                .extracting(NotificationDelivery::getNotification)
                .containsOnly(notificationCaptor.getValue());
        assertThat(deliveryCaptor.getAllValues())
                .extracting(NotificationDelivery::getWebPushSubscription)
                .containsExactlyInAnyOrder(desktopSubscription, mobileSubscription);
        verify(webPushSubscriptionRepository).findByUserIdAndIsActiveTrue(USER_ID);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                notificationRepository,
                notificationDeliveryRepository
        );
        inOrder.verify(notificationRepository).save(any(Notification.class));
        inOrder.verify(notificationDeliveryRepository, times(2))
                .save(any(NotificationDelivery.class));
    }

    @Test
    @DisplayName("활성 Web Push 구독이 없으면 Notification만 저장한다")
    void saveNotificationWithoutDeliveryWhenNoActiveSubscription() {
        Notice notice = mock(Notice.class);
        Study study = mock(Study.class);
        StudyMember recipient = mock(StudyMember.class);
        User recipientUser = mock(User.class);
        when(notice.getStudy()).thenReturn(study);
        when(notice.getId()).thenReturn(NOTICE_ID);
        when(notice.getTitle()).thenReturn("공지 제목");
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipientUser.getId()).thenReturn(USER_ID);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);
        when(webPushSubscriptionRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of());

        notificationService.createNoticeCreatedEventNotifications(notice, List.of(recipient));

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationDeliveryRepository, never()).save(any(NotificationDelivery.class));
        verify(webPushSubscriptionRepository).findByUserIdAndIsActiveTrue(USER_ID);
    }

    @Test
    @DisplayName("대상이 되는 리마인더가 없으면 알림을 생성하지 않는다")
    void doNothingWhenNoPendingDueReminder() {
        when(assignmentReminderRepository.findClaimableForUpdate(NOW, 100)).thenReturn(List.of());
        when(noticeReminderRepository.findClaimableForUpdate(NOW, 100)).thenReturn(List.of());

        notificationService.createScheduledRemindNotifications();

        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(noticeRecipientRepository, assignmentSubmissionRepository);
    }
}
