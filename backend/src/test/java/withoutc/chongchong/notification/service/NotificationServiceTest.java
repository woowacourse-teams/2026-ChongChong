package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.AssignmentReminderRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.notice.entity.Notice;
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
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long NOTICE_ID = 100L;
    private static final Long ASSIGNMENT_ID = 200L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long NOTIFICATION_ID = 300L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

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

    @Test
    @DisplayName("공지 댓글은 작성자를 제외하고 사용자별 한 번 알림과 활성 구독 Delivery를 만든다")
    void createNoticeCommentNotifications() {
        Study study = Study.create("자바 스터디", "설명", false);
        ReflectionTestUtils.setField(study, "id", 5L);
        User authorUser = User.create("작성자", null);
        ReflectionTestUtils.setField(authorUser, "id", USER_ID);
        User leaderUser = User.create("리더", null);
        ReflectionTestUtils.setField(leaderUser, "id", OTHER_USER_ID);
        StudyMember author = StudyMember.create(study, authorUser, "작성자", null, StudyMemberRole.MEMBER);
        StudyMember leader = StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER);
        Notice notice = Notice.create(study, "원문 제목", "내용");
        ReflectionTestUtils.setField(notice, "id", NOTICE_ID);
        Comment comment = Comment.create(ResourceType.NOTICE, NOTICE_ID, author, "댓글 본문");
        ReflectionTestUtils.setField(comment, "id", 42L);
        WebPushSubscription subscription = mock(WebPushSubscription.class);
        when(webPushSubscriptionRepository.findByUserIdAndIsActiveTrue(OTHER_USER_ID))
                .thenReturn(List.of(subscription));

        notificationService.createCommentCreatedEventNotifications(comment, notice.getStudy(), author,
                List.of(author, leader, leader));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.getTitle()).isEqualTo("[자바 스터디] 새 댓글");
        assertThat(notification.getBody()).isEqualTo("작성자 스터디원이 댓글을 달았어요");
        assertThat(notification.getDeepLink()).isEqualTo("/studies/5/notices/100?focusCommentId=42");
        assertThat(notification.getComment()).isSameAs(comment);
        assertThat(notification.getRecipientMember()).isSameAs(leader);
        assertThat(notification.getResourceType()).isEqualTo(ResourceType.NOTICE);
        assertThat(notification.getResourceId()).isEqualTo(NOTICE_ID);
        verify(notificationDeliveryRepository).save(any(NotificationDelivery.class));
        verify(webPushSubscriptionRepository, never()).findByUserIdAndIsActiveTrue(USER_ID);
    }

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationDeliveryRepository,
                webPushSubscriptionRepository,
                noticeReminderRepository,
                noticeRepository,
                assignmentSubmissionRepository,
                assignmentReminderRepository,
                assignmentRepository
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
    @DisplayName("과제 생성자는 과제 생성 알림과 Web Push 발송 대상에서 제외한다")
    void excludeAssignmentCreatorFromCreatedEventNotifications() {
        Assignment assignment = mock(Assignment.class);
        Study study = mock(Study.class);
        StudyMember creator = mock(StudyMember.class);
        StudyMember recipient = mock(StudyMember.class);
        User creatorUser = mock(User.class);
        User recipientUser = mock(User.class);
        WebPushSubscription recipientSubscription = mock(WebPushSubscription.class);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(assignment.getStudy()).thenReturn(study);
        when(assignment.getTitle()).thenReturn("과제 제목");
        when(study.getId()).thenReturn(1L);
        when(study.getName()).thenReturn("스터디");
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(creator.getUser()).thenReturn(creatorUser);
        when(creatorUser.getId()).thenReturn(USER_ID);
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipientUser.getId()).thenReturn(OTHER_USER_ID);
        when(webPushSubscriptionRepository.findByUserIdAndIsActiveTrue(OTHER_USER_ID))
                .thenReturn(List.of(recipientSubscription));

        notificationService.createAssignmentCreatedEventNotifications(
                assignment, List.of(creator, recipient), USER_ID
        );

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getRecipient()).isSameAs(recipientUser);
        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepository).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue().getNotification()).isSameAs(notificationCaptor.getValue());
        assertThat(deliveryCaptor.getValue().getWebPushSubscription()).isSameAs(recipientSubscription);
        verify(webPushSubscriptionRepository).findByUserIdAndIsActiveTrue(OTHER_USER_ID);
        verify(webPushSubscriptionRepository, never()).findByUserIdAndIsActiveTrue(USER_ID);
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

}
