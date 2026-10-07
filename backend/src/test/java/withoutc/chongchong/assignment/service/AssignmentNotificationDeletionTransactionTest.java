package withoutc.chongchong.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
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
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assignment-notification-deletion-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class AssignmentNotificationDeletionTransactionTest {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanBeforeTest() {
        databaseCleaner.clean();
    }

    @AfterEach
    void cleanAfterTest() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("과제 삭제 시 과제·제출물 알림과 발송 기록을 함께 삭제한다")
    void deleteAssignmentRemovesNotificationsAndDeliveries() {
        Fixture fixture = createFixture();

        assignmentService.delete(fixture.leader().getUser().getId(), fixture.study().getId(),
                fixture.assignment().getId());

        assertThat(assignmentRepository.existsById(fixture.assignment().getId())).isFalse();
        assertThat(notificationRepository.existsById(fixture.assignmentNotification().getId())).isFalse();
        assertThat(notificationRepository.existsById(fixture.submissionNotification().getId())).isFalse();
        assertThat(notificationDeliveryRepository.existsById(fixture.assignmentDelivery().getId())).isFalse();
        assertThat(notificationDeliveryRepository.existsById(fixture.submissionDelivery().getId())).isFalse();
    }

    @Test
    @DisplayName("과제 삭제가 롤백되면 과제·알림·발송 기록을 모두 보존한다")
    void rollbackAssignmentDeletionRestoresNotificationsAndDeliveries() {
        Fixture fixture = createFixture();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            assignmentService.delete(fixture.leader().getUser().getId(), fixture.study().getId(),
                    fixture.assignment().getId());
            throw new IllegalStateException("강제 롤백");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("강제 롤백");

        assertThat(assignmentRepository.existsById(fixture.assignment().getId())).isTrue();
        assertThat(assignmentSubmissionRepository.existsById(fixture.submission().getId())).isTrue();
        assertThat(notificationRepository.existsById(fixture.assignmentNotification().getId())).isTrue();
        assertThat(notificationRepository.existsById(fixture.submissionNotification().getId())).isTrue();
        assertThat(notificationDeliveryRepository.existsById(fixture.assignmentDelivery().getId())).isTrue();
        assertThat(notificationDeliveryRepository.existsById(fixture.submissionDelivery().getId())).isTrue();
    }

    private Fixture createFixture() {
        User leaderUser = userRepository.saveAndFlush(User.create("리더", null));
        User memberUser = userRepository.saveAndFlush(User.create("스터디원", null));
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명"));
        StudyMember leader = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER));
        StudyMember member = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, memberUser, "스터디원", null, StudyMemberRole.MEMBER));
        LocalDateTime now = LocalDateTime.now();
        Assignment assignment = Assignment.create(study, "과제", "내용", "링크",
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY, now.plusDays(1), now);
        assignment.initializeSubmissions(List.of(member));
        assignmentRepository.saveAndFlush(assignment);
        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignmentIdAndMemberId(assignment.getId(), member.getId())
                .orElseThrow();

        Notification assignmentNotification = saveNotification(
                leaderUser, assignment.getId(), ResourceType.ASSIGNMENT);
        Notification submissionNotification = saveNotification(
                leaderUser, submission.getId(), ResourceType.ASSIGNMENT_SUBMISSION);
        WebPushSubscription subscription = webPushSubscriptionRepository.saveAndFlush(WebPushSubscription.create(
                leaderUser,
                "test-installation",
                "https://push.example.com/assignment-notification-deletion",
                "p256dh-key",
                "auth-secret"
        ));
        NotificationDelivery assignmentDelivery = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(assignmentNotification, subscription));
        NotificationDelivery submissionDelivery = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(submissionNotification, subscription));

        return new Fixture(study, leader, assignment, submission,
                assignmentNotification, submissionNotification, assignmentDelivery, submissionDelivery);
    }

    private Notification saveNotification(User recipient, Long resourceId, ResourceType resourceType) {
        return notificationRepository.saveAndFlush(Notification.create(
                recipient,
                "[스터디] 알림",
                "알림 내용",
                NotificationType.NEW,
                resourceId,
                resourceType,
                "/studies/1/assignments/1"
        ));
    }

    private record Fixture(
            Study study,
            StudyMember leader,
            Assignment assignment,
            AssignmentSubmission submission,
            Notification assignmentNotification,
            Notification submissionNotification,
            NotificationDelivery assignmentDelivery,
            NotificationDelivery submissionDelivery
    ) {
    }
}
