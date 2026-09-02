package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StudyMemberRemovalAcceptanceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 9, 0);

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeRecipientRepository noticeRecipientRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private StudyInviteTokenProvider studyInviteTokenProvider;

    @LocalServerPort
    private int port;

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("활동 내역이 있는 멤버를 방출하면 대상의 연관 데이터만 삭제한다")
    void expelMemberWithActivityDataTest() {
        RemovalFixture fixture = createRemovalFixture();

        testAuthRequest.givenAuthenticatedUser(fixture.leader().getUser().getId())
                .port(port)
                .when()
                .delete(
                        "/studies/{studyId}/members/{memberId}",
                        fixture.study().getId(),
                        fixture.target().getId()
                )
                .then()
                .statusCode(204);

        assertOnlyTargetRemoved(fixture);
    }

    @Test
    @DisplayName("활동 내역이 있는 멤버가 탈퇴하면 자신의 연관 데이터만 삭제한다")
    void leaveMemberWithActivityDataTest() {
        RemovalFixture fixture = createRemovalFixture();

        testAuthRequest.givenAuthenticatedUser(fixture.target().getUser().getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/me", fixture.study().getId())
                .then()
                .statusCode(204);

        assertOnlyTargetRemoved(fixture);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 같은 초대 토큰으로 스터디에 다시 가입할 수 있다")
    void leaveAndRejoinStudyTest() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember member = createMember(study, "재가입 멤버", StudyMemberRole.MEMBER);
        Long removedMemberId = member.getId();
        Long userId = member.getUser().getId();
        String inviteToken = studyInviteTokenProvider.generate(study.getId());

        testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/me", study.getId())
                .then()
                .statusCode(204);

        testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {"token": "%s"}
                        """.formatted(inviteToken))
                .when()
                .post("/studies/join")
                .then()
                .statusCode(201);

        assertThat(studyMemberRepository.findByStudyIdAndUserId(study.getId(), userId))
                .get()
                .satisfies(rejoinedMember -> {
                    assertThat(rejoinedMember.getId()).isNotEqualTo(removedMemberId);
                    assertThat(rejoinedMember.getRole()).isEqualTo(StudyMemberRole.MEMBER);
                });
    }

    private RemovalFixture createRemovalFixture() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember target = createMember(study, "삭제 대상", StudyMemberRole.MEMBER);
        StudyMember otherMember = createMember(study, "다른 멤버", StudyMemberRole.MEMBER);

        Notice notice = Notice.create(study, "공지", "공지 내용");
        notice.addRecipients(List.of(target, otherMember));
        notice.getRecipients().stream()
                .filter(recipient -> recipient.getMember().getId().equals(target.getId()))
                .findFirst()
                .orElseThrow()
                .markAsRead(NOW);
        noticeRepository.saveAndFlush(notice);

        Assignment assignment = Assignment.create(
                study,
                "과제",
                "과제 내용",
                "링크 제출",
                NOW.plusDays(1),
                NOW
        );
        assignment.initializeSubmissions(List.of(target, otherMember));
        assignment.getSubmissions().stream()
                .filter(submission -> submission.getMember().getId().equals(target.getId()))
                .findFirst()
                .orElseThrow()
                .submit("제출 내용", null, NOW);
        assignmentRepository.saveAndFlush(assignment);

        saveNotification(study, target, notice.getId(), NotificationResourceType.NOTICE);
        saveNotification(study, target, assignment.getId(), NotificationResourceType.ASSIGNMENT);
        saveNotification(study, otherMember, notice.getId(), NotificationResourceType.NOTICE);
        saveNotification(study, otherMember, assignment.getId(), NotificationResourceType.ASSIGNMENT);

        return new RemovalFixture(study, leader, target, otherMember, notice, assignment);
    }

    private void assertOnlyTargetRemoved(RemovalFixture fixture) {
        Long targetId = fixture.target().getId();
        Long otherMemberId = fixture.otherMember().getId();

        assertThat(studyMemberRepository.findById(targetId)).isEmpty();
        assertThat(studyMemberRepository.findById(fixture.leader().getId())).isPresent();
        assertThat(studyMemberRepository.findById(otherMemberId)).isPresent();

        assertThat(notificationRepository.findAll())
                .hasSize(2)
                .allSatisfy(notification ->
                        assertThat(notification.getRecipient().getId()).isEqualTo(otherMemberId));
        assertThat(noticeRecipientRepository.findByNoticeIdAndMemberId(fixture.notice().getId(), targetId))
                .isEmpty();
        assertThat(noticeRecipientRepository.findByNoticeIdAndMemberId(fixture.notice().getId(), otherMemberId))
                .isPresent();
        assertThat(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(
                fixture.assignment().getId(), targetId)).isEmpty();
        assertThat(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(
                fixture.assignment().getId(), otherMemberId)).isPresent();

        assertThat(noticeRepository.findById(fixture.notice().getId())).isPresent();
        assertThat(assignmentRepository.findById(fixture.assignment().getId())).isPresent();
        assertThat(studyRepository.findById(fixture.study().getId())).isPresent();

        Long targetUserId = fixture.target().getUser().getId();
        testAuthRequest.givenAuthenticatedUser(targetUserId)
                .port(port)
                .when()
                .get("/studies/{studyId}/members", fixture.study().getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
        testAuthRequest.givenAuthenticatedUser(targetUserId)
                .port(port)
                .when()
                .get("/studies/me")
                .then()
                .statusCode(200)
                .body("studyCount", equalTo(0))
                .body("studies", hasSize(0));
    }

    private StudyMember createMember(Study study, String name, StudyMemberRole role) {
        User user = userRepository.saveAndFlush(User.create(name, null));
        return studyMemberRepository.saveAndFlush(StudyMember.create(study, user, name, null, role));
    }

    private void saveNotification(
            Study study,
            StudyMember recipient,
            Long resourceId,
            NotificationResourceType resourceType
    ) {
        Notification notification = BeanUtils.instantiateClass(Notification.class);
        ReflectionTestUtils.setField(notification, "study", study);
        ReflectionTestUtils.setField(notification, "recipient", recipient);
        ReflectionTestUtils.setField(notification, "type", NotificationType.REMIND);
        ReflectionTestUtils.setField(notification, "resourceId", resourceId);
        ReflectionTestUtils.setField(notification, "resourceType", resourceType);
        notificationRepository.saveAndFlush(notification);
    }

    private record RemovalFixture(
            Study study,
            StudyMember leader,
            StudyMember target,
            StudyMember otherMember,
            Notice notice,
            Assignment assignment
    ) {
    }
}
