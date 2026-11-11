package withoutc.chongchong.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.service.StudyMemberService;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3PresignerConfiguration.class)
class CommentHomeProfileImageApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private StudyMemberRepository members;
    @Autowired
    private NoticeRepository notices;
    @Autowired
    private AssignmentRepository assignments;
    @Autowired
    private AssignmentSubmissionRepository submissions;
    @Autowired
    private CommentRepository comments;
    @Autowired
    private FileMetaDataRepository files;
    @Autowired
    private StudyMemberService memberService;
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
    private User author;
    private User reader;
    private Study study;
    private StudyMember authorMember;
    private StudyMember readerMember;
    private Notice notice;
    private Assignment assignment;
    private AssignmentSubmission submission;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = users.saveAndFlush(User.create("리더", null));
        author = users.saveAndFlush(User.create("홈 이름", "https://example.com/home.png"));
        reader = users.saveAndFlush(User.create("독자", null));
        study = studies.saveAndFlush(Study.create("스터디", "설명", false));
        saveMember(leader, "리더", null, StudyMemberRole.LEADER);
        authorMember = saveMember(author, "스터디 작성자", "https://example.com/stale.png", StudyMemberRole.MEMBER);
        readerMember = saveMember(reader, "독자", null, StudyMemberRole.MEMBER);
        notice = notices.saveAndFlush(Notice.create(study, "공지", "내용"));
        LocalDateTime now = LocalDateTime.now(clock);
        assignment = assignments.saveAndFlush(Assignment.create(study, "과제", "내용", "안내",
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY, now.plusDays(7), now));
        submission = AssignmentSubmission.create(authorMember, assignment);
        submission.submit("제출 내용", null, now);
        submissions.saveAndFlush(submission);
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @ParameterizedTest
    @EnumSource(ResourceType.class)
    @DisplayName("공지·과제·제출물 댓글은 홈 이미지 교체·초기화를 반영하고 스터디 작성자 이름을 유지한다")
    void reflectsHomeImageChanges(ResourceType type) {
        Comment comment = saveComment(type, authorMember, "댓글 내용");
        assertAuthorImage(item(list(type), comment.getId()), "https://example.com/home.png", false);

        FileMetadata first = saveImage(author);
        changeHomeImage(first);
        assertAuthorImage(item(list(type), comment.getId()), first.getObjectKey(), true);

        FileMetadata replacement = saveImage(author);
        changeHomeImage(replacement);
        assertAuthorImage(item(list(type), comment.getId()), replacement.getObjectKey(), true);
        Response reset = auth.givenAuthenticatedUser(author.getId()).port(port).delete("/users/me/profile-image");
        assertThat(reset.statusCode()).isEqualTo(204);
        clearInvocations(urlGenerator);
        Map<String, Object> result = item(list(type), comment.getId());
        assertAuthorImage(result, null, false);
        assertThat(result).containsEntry("content", "댓글 내용").containsEntry("canEdit", false)
                .containsEntry("canDelete", true).containsEntry("isEdited", false);
        verifyNoInteractions(urlGenerator);
    }

    @ParameterizedTest
    @EnumSource(value = ResourceType.class, names = {"NOTICE", "ASSIGNMENT"})
    @DisplayName("홈 이미지가 남아 있어도 탈퇴·재가입 전 댓글은 탈퇴자로 유지하고 새 댓글만 현재 이미지를 사용한다")
    void keepsDepartedAuthorMaskedAfterRejoining(ResourceType type) {
        Comment old = saveComment(type, authorMember, "예전 댓글");
        FileMetadata file = bindImage(author);

        memberService.leave(author.getId(), study.getId());

        assertDeparted(item(list(type), old.getId()));
        StudyMember rejoined = saveMember(author, "재가입 이름", "https://example.com/rejoined.png", StudyMemberRole.MEMBER);
        assertThat(rejoined.getId()).isNotEqualTo(authorMember.getId());
        assertDeparted(item(list(type), old.getId()));
        assertThat(jdbc.queryForObject("SELECT profile_image_file_id FROM users WHERE id = ?", Long.class, author.getId()))
                .isEqualTo(file.getId());
        verifyNoInteractions(urlGenerator);

        Comment fresh = saveComment(type, rejoined, "새 댓글");
        Response response = list(type);

        assertDeparted(item(response, old.getId()));
        Map<String, Object> freshAuthor = author(item(response, fresh.getId()));
        assertThat(freshAuthor).containsEntry("name", "재가입 이름").containsEntry("isDeparted", false);
        assertThat((String) freshAuthor.get("profileImageUrl")).contains(file.getObjectKey(), "X-Amz-Signature=");
        verify(urlGenerator).generateImageUrl(file.getObjectKey(), "image/png");
    }

    @Test
    @DisplayName("같은 페이지의 반복 작성자는 한 번만 서명하고 다음 페이지 작성자는 미리 서명하지 않는다")
    void signsDistinctAuthorsOnVisiblePageOnly() {
        FileMetadata firstImage = bindImage(author);
        FileMetadata nextImage = bindImage(reader);
        Comment first = saveComment(ResourceType.NOTICE, authorMember, "첫 댓글");
        Comment second = saveComment(ResourceType.NOTICE, authorMember, "둘째 댓글");
        Comment next = saveComment(ResourceType.NOTICE, readerMember, "다음 댓글");

        Response page = successful(auth.givenAuthenticatedUser(leader.getId()).port(port).queryParam("size", 2)
                .get(path(ResourceType.NOTICE)));

        assertThat(page.jsonPath().getList("comments.id", Long.class)).containsExactly(first.getId(), second.getId());
        assertThat(page.jsonPath().getBoolean("hasNext")).isTrue();
        assertThat(page.jsonPath().getLong("nextCursor")).isEqualTo(second.getId());
        verify(urlGenerator).generateImageUrl(firstImage.getObjectKey(), "image/png");
        verify(urlGenerator, never()).generateImageUrl(nextImage.getObjectKey(), "image/png");
        clearInvocations(urlGenerator);

        Response lastPage = successful(auth.givenAuthenticatedUser(leader.getId()).port(port)
                .queryParam("size", 2).queryParam("cursor", second.getId()).get(path(ResourceType.NOTICE)));

        assertThat(lastPage.jsonPath().getList("comments.id", Long.class)).containsExactly(next.getId());
        assertThat(lastPage.jsonPath().getBoolean("hasNext")).isFalse();
        verify(urlGenerator).generateImageUrl(nextImage.getObjectKey(), "image/png");
        verify(urlGenerator, never()).generateImageUrl(firstImage.getObjectKey(), "image/png");
    }

    @Test
    @DisplayName("인증·소속·제출물 공개 범위·댓글 위치 확인 실패 시 이미지를 해석하지 않는다")
    void checksAccessBeforeSigning() {
        bindImage(author);
        User outsider = users.saveAndFlush(User.create("외부 사용자", null));
        Comment anotherResource = saveComment(ResourceType.ASSIGNMENT, authorMember, "과제 댓글");

        assertThat(given().port(port).get("/api" + path(ResourceType.NOTICE)).statusCode()).isEqualTo(401);
        assertThat(auth.givenAuthenticatedUser(outsider.getId()).port(port).get(path(ResourceType.NOTICE)).statusCode())
                .isEqualTo(403);
        assertThat(auth.givenAuthenticatedUser(reader.getId()).port(port)
                .get(path(ResourceType.ASSIGNMENT_SUBMISSION)).statusCode()).isEqualTo(403);
        assertThat(auth.givenAuthenticatedUser(leader.getId()).port(port)
                .queryParam("focusCommentId", anotherResource.getId()).get(path(ResourceType.NOTICE)).statusCode())
                .isEqualTo(404);
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("빈 댓글 목록에서는 프로필 조회 URL을 생성하지 않는다")
    void readsEmptyCommentPage() {
        bindImage(author);

        Response response = list(ResourceType.NOTICE);

        assertThat(response.jsonPath().getList("comments")).isEmpty();
        assertThat(response.jsonPath().getBoolean("hasNext")).isFalse();
        assertThat(response.jsonPath().getString("nextCursor")).isNull();
        verifyNoInteractions(urlGenerator);
    }

    private Comment saveComment(ResourceType type, StudyMember member, String content) {
        Long resourceId = switch (type) {
            case NOTICE -> notice.getId();
            case ASSIGNMENT -> assignment.getId();
            case ASSIGNMENT_SUBMISSION -> submission.getId();
        };
        return comments.saveAndFlush(Comment.create(type, resourceId, member, content));
    }

    private String path(ResourceType type) {
        String studyPath = "/studies/" + study.getId();
        return switch (type) {
            case NOTICE -> studyPath + "/notices/" + notice.getId() + "/comments";
            case ASSIGNMENT -> studyPath + "/assignments/" + assignment.getId() + "/comments";
            case ASSIGNMENT_SUBMISSION -> studyPath + "/assignments/" + assignment.getId()
                    + "/submissions/" + submission.getId() + "/comments";
        };
    }

    private Response list(ResourceType type) {
        return successful(auth.givenAuthenticatedUser(leader.getId()).port(port).get(path(type)));
    }

    private Response successful(Response response) {
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private Map<String, Object> item(Response response, Long id) {
        List<Map<String, Object>> items = response.jsonPath().getList("comments");
        return items.stream().filter(item -> ((Number) item.get("id")).longValue() == id).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> author(Map<String, Object> comment) {
        return (Map<String, Object>) comment.get("author");
    }

    private void assertAuthorImage(Map<String, Object> comment, String expected, boolean signed) {
        Map<String, Object> author = author(comment);
        assertThat(author).containsEntry("name", "스터디 작성자").containsEntry("isDeparted", false);
        if (signed) {
            assertThat((String) author.get("profileImageUrl")).startsWith("https://")
                    .contains(expected, "X-Amz-Signature=");
            return;
        }
        assertThat(author).containsEntry("profileImageUrl", expected);
    }

    private void assertDeparted(Map<String, Object> comment) {
        assertThat(author(comment)).containsEntry("name", "탈퇴한 스터디원")
                .containsEntry("profileImageUrl", null).containsEntry("isDeparted", true);
    }

    private StudyMember saveMember(User user, String name, String image, StudyMemberRole role) {
        return members.saveAndFlush(StudyMember.create(study, user, name, image, role));
    }

    private FileMetadata saveImage(User owner) {
        FileMetadata file = FileMetadata.create(owner, "avatar.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return files.saveAndFlush(file);
    }

    private FileMetadata bindImage(User owner) {
        FileMetadata file = saveImage(owner);
        owner.changeProfileImage(file);
        users.saveAndFlush(owner);
        return file;
    }

    private void changeHomeImage(FileMetadata file) {
        Response response = auth.givenAuthenticatedUser(author.getId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("fileId", file.getId())).patch("/users/me/profile-image");
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
