package withoutc.chongchong.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3PresignerConfiguration.class)
class AssignmentHomeProfileImageApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository memberRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private AssignmentSubmissionRepository submissionRepository;
    @Autowired
    private FileMetaDataRepository fileRepository;
    @Autowired
    private TestAuthRequest auth;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Clock clock;

    @MockitoSpyBean
    private FileUrlGenerator urlGenerator;

    @LocalServerPort
    private int port;

    private User leader;
    private User target;
    private User other;
    private Study study;
    private StudyMember targetMember;
    private StudyMember otherMember;
    private Assignment assignment;
    private AssignmentSubmission submitted;
    private AssignmentSubmission pending;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.saveAndFlush(User.create("리더", null));
        target = userRepository.saveAndFlush(User.create("홈 이름", "https://example.com/home.png"));
        other = userRepository.saveAndFlush(User.create("다른 사용자", "https://example.com/other.png"));
        study = studyRepository.saveAndFlush(Study.create("스터디", "설명", false));
        saveMember(leader, "리더", null, StudyMemberRole.LEADER);
        targetMember = saveMember(target, "스터디 이름", "https://example.com/stale.png", StudyMemberRole.MEMBER);
        otherMember = saveMember(other, "다른 멤버", "https://example.com/stale-other.png", StudyMemberRole.MEMBER);
        assignment = createAssignment();
        submitted = AssignmentSubmission.create(targetMember, assignment);
        submitted.submit("제출 내용", "https://example.com/result", LocalDateTime.now(clock).minusMinutes(1));
        submissionRepository.saveAndFlush(submitted);
        pending = submissionRepository.saveAndFlush(AssignmentSubmission.create(otherMember, assignment));
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("제출 현황·목록·상세는 멤버의 복사된 URL 대신 현재 홈 외부 URL을 표시한다")
    void readsHomeExternalImagesAndPreservesSubmissionData() {
        assertTargetImages("https://example.com/home.png", false);
        Response status = statuses();
        assertThat(image(status, "incompleteMembers", otherMember.getId())).isEqualTo("https://example.com/other.png");
        assertThat(status.jsonPath().getInt("memberCount")).isEqualTo(2);
        assertThat(status.jsonPath().getInt("completeCount")).isEqualTo(1);
        assertThat(status.jsonPath().getInt("incompleteCount")).isEqualTo(1);
        Response list = successful(submissions(leader));
        assertThat(list.jsonPath().getList("submissions")).hasSize(1);
        assertThat(list.jsonPath().getString("submissions[0].name")).isEqualTo("스터디 이름");
        assertThat(list.jsonPath().getString("submissions[0].submissionStatus")).isEqualTo("SUBMITTED");
        Response detail = successful(detail(target, submitted.getId()));
        assertThat(detail.jsonPath().getString("content")).isEqualTo("제출 내용");
        assertThat(detail.jsonPath().getString("link")).isEqualTo("https://example.com/result");
        assertThat(detail.jsonPath().getString("createdAt")).isNotBlank();
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("홈 업로드 이미지 교체가 세 조회에 반영되고 다른 제출자의 이미지는 유지된다")
    void reflectsUploadedImageReplacement() {
        FileMetadata first = saveImage();
        changeHomeImage(first);
        assertTargetImages(first.getObjectKey(), true);

        FileMetadata replacement = saveImage();
        changeHomeImage(replacement);
        assertTargetImages(replacement.getObjectKey(), true);
        assertThat(image(statuses(), "incompleteMembers", otherMember.getId())).isEqualTo("https://example.com/other.png");
    }

    @Test
    @DisplayName("홈 이미지 초기화는 세 조회에 null을 반영하며 기존 멤버 URL을 복원하지 않는다")
    void reflectsResetInSubmissionReads() {
        FileMetadata file = saveImage();
        changeHomeImage(file);
        Response reset = auth.givenAuthenticatedUser(target.getId()).port(port).delete("/users/me/profile-image");
        assertThat(reset.statusCode()).isEqualTo(204);
        clearInvocations(urlGenerator);

        assertTargetImages(null, false);

        assertThat(jdbc.queryForObject("SELECT profile_image_url FROM study_members WHERE id = ?", String.class,
                targetMember.getId())).isEqualTo("https://example.com/stale.png");
        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isFalse();
        verifyNoInteractions(urlGenerator);
    }

    @ParameterizedTest
    @EnumSource(SubmissionVisibility.class)
    @DisplayName("다른 멤버의 목록·완료 상세는 공개 범위에 따라 허용하며 본인 상세는 계속 조회할 수 있다")
    void preservesVisibilityRules(SubmissionVisibility visibility) {
        bindImageWithoutSigning();
        changeVisibility(visibility);
        int expectedStatus = visibility == SubmissionVisibility.LEADER_ONLY ? 403 : 200;

        assertThat(submissions(other).statusCode()).isEqualTo(expectedStatus);
        assertThat(detail(other, submitted.getId()).statusCode()).isEqualTo(expectedStatus);
        if (expectedStatus == 403) {
            verifyNoInteractions(urlGenerator);
        }
        assertThat(detail(target, submitted.getId()).statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("공개 과제여도 미제출 상세는 본인과 리더만 조회하며 미제출 데이터의 null을 유지한다")
    void preservesUnsubmittedPrivacyAndNullFields() {
        changeVisibility(SubmissionVisibility.ALL_STUDY_MEMBERS);

        assertThat(detail(target, pending.getId()).statusCode()).isEqualTo(403);
        Response detail = successful(detail(other, pending.getId()));

        assertThat(detail.jsonPath().getString("profileImage")).isEqualTo("https://example.com/other.png");
        assertThat(detail.jsonPath().getMap("$"))
                .containsEntry("createdAt", null).containsEntry("content", null).containsEntry("link", null);
        assertThat(successful(submissions(other)).jsonPath().getList("submissions")).hasSize(1);
    }

    @Test
    @DisplayName("인증·스터디 접근·리더 권한·제출물 소속 확인 실패 시 프로필 URL을 발급하지 않는다")
    void rejectsUnauthorizedReadsBeforeSigning() {
        bindImageWithoutSigning();
        User outsider = userRepository.saveAndFlush(User.create("외부 사용자", null));
        assertThat(given().port(port).get("/api/studies/{studyId}/assignments/{id}/submissions",
                study.getId(), assignment.getId()).statusCode()).isEqualTo(401);
        assertThat(submissions(outsider).statusCode()).isEqualTo(403);
        assertThat(auth.givenAuthenticatedUser(target.getId()).port(port)
                .get("/studies/{studyId}/assignments/{id}/status", study.getId(), assignment.getId()).statusCode())
                .isEqualTo(403);
        Assignment otherAssignment = createAssignment();
        assertThat(auth.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{studyId}/assignments/{id}/submissions/{submissionId}", study.getId(),
                        otherAssignment.getId(), submitted.getId()).statusCode()).isEqualTo(404);
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("제출 대상이 없는 과제의 현황·목록은 빈 응답을 유지한다")
    void readsEmptySubmissionLists() {
        Assignment empty = createAssignment();
        Response status = auth.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{studyId}/assignments/{id}/status", study.getId(), empty.getId());
        Response list = auth.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{studyId}/assignments/{id}/submissions", study.getId(), empty.getId());

        assertThat(status.statusCode()).isEqualTo(200);
        assertThat(status.jsonPath().getInt("memberCount")).isZero();
        assertThat(status.jsonPath().getList("completeMembers")).isEmpty();
        assertThat(status.jsonPath().getList("incompleteMembers")).isEmpty();
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.jsonPath().getList("submissions")).isEmpty();
        verifyNoInteractions(urlGenerator);
    }

    private void assertTargetImages(String expected, boolean signed) {
        assertImage(image(statuses(), "completeMembers", targetMember.getId()), expected, signed);
        assertImage(image(successful(submissions(leader)), "submissions", submitted.getId()), expected, signed);
        assertImage(successful(detail(leader, submitted.getId())).jsonPath().getString("profileImage"), expected, signed);
        assertImage(successful(detail(target, submitted.getId())).jsonPath().getString("profileImage"), expected, signed);
    }

    private void assertImage(String actual, String expected, boolean signed) {
        if (signed) {
            assertThat(actual).startsWith("https://").contains(expected, "X-Amz-Signature=");
            return;
        }
        assertThat(actual).isEqualTo(expected);
    }

    private String image(Response response, String path, Long id) {
        List<Map<String, Object>> items = response.jsonPath().getList(path);
        return (String) items.stream().filter(item -> ((Number) item.get("id")).longValue() == id)
                .findFirst().orElseThrow().get("profileImage");
    }

    private Response statuses() {
        return successful(auth.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{studyId}/assignments/{id}/status", study.getId(), assignment.getId()));
    }

    private Response submissions(User actor) {
        return auth.givenAuthenticatedUser(actor.getId()).port(port)
                .get("/studies/{studyId}/assignments/{id}/submissions", study.getId(), assignment.getId());
    }

    private Response detail(User actor, Long submissionId) {
        return auth.givenAuthenticatedUser(actor.getId()).port(port)
                .get("/studies/{studyId}/assignments/{id}/submissions/{submissionId}",
                        study.getId(), assignment.getId(), submissionId);
    }

    private Response successful(Response response) {
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private Assignment createAssignment() {
        LocalDateTime now = LocalDateTime.now(clock);
        return assignmentRepository.saveAndFlush(Assignment.create(study, "과제", "내용", "안내",
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY, now.plusDays(7), now));
    }

    private StudyMember saveMember(User user, String name, String image, StudyMemberRole role) {
        return memberRepository.saveAndFlush(StudyMember.create(study, user, name, image, role));
    }

    private FileMetadata saveImage() {
        FileMetadata file = FileMetadata.create(target, "avatar.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private void bindImageWithoutSigning() {
        FileMetadata file = saveImage();
        target.changeProfileImage(file);
        userRepository.saveAndFlush(target);
    }

    private void changeHomeImage(FileMetadata file) {
        Response response = auth.givenAuthenticatedUser(target.getId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("fileId", file.getId())).patch("/users/me/profile-image");
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private void changeVisibility(SubmissionVisibility visibility) {
        Response response = auth.givenAuthenticatedUser(leader.getId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("submissionVisibility", visibility.name()))
                .patch("/studies/{studyId}/assignments/{id}", study.getId(), assignment.getId());
        assertThat(response.statusCode()).isEqualTo(204);
    }
}
