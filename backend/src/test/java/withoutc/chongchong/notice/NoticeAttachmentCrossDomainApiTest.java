package withoutc.chongchong.notice;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import withoutc.chongchong.assignment.service.AssignmentAttachmentService;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.service.NoticeAttachmentService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "notice.attachments.max-count=10",
        "notice.attachments.max-size-bytes=5242880",
        "notice.attachments.allowed-content-types=image/jpeg,image/png,image/webp"
})
class NoticeAttachmentCrossDomainApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @MockitoSpyBean
    private NoticeAttachmentService noticeAttachmentService;

    @MockitoSpyBean
    private AssignmentAttachmentService assignmentAttachmentService;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private User leader;
    private Long studyId;
    private Long noticeId;
    private Long assignmentId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("리더", null));
        User member = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("교차 도메인 첨부 스터디", "설명", false));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        studyId = study.getId();
        Response notice = post("/studies/" + studyId + "/notices", Map.of("title", "기존 제목", "content", "기존 내용"));
        notice.then().statusCode(201);
        noticeId = notice.jsonPath().getLong("noticeId");
        Response assignment = post("/studies/" + studyId + "/assignments", Map.of(
                "title", "과제 제목", "content", "과제 내용", "submissionTarget", "MEMBERS_AND_LEADER",
                "submissionVisibility", "LEADER_ONLY", "closeAt", "2099-12-31T23:59:00"));
        assignment.then().statusCode(201);
        assignmentId = assignment.jsonPath().getLong("assignmentId");
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @ParameterizedTest
    @EnumSource(OtherDomain.class)
    @DisplayName("과제·제출물에 연결된 완료 파일은 공지 수정에 추가할 수 없고 기존 공지를 보존한다")
    void rejectFileAlreadyAttachedToOtherDomain(OtherDomain domain) {
        FileMetadata file = saveCompletedFile();
        attachToOtherDomain(domain, List.of(file.getId())).then().statusCode(domain.successStatus);

        assertNotAttachable(patchNotice(List.of(file.getId())));

        assertNoticeUnchanged();
        assertOnlyLinkedTo(file, domain.table);
    }

    @ParameterizedTest
    @EnumSource(OtherDomain.class)
    @DisplayName("공지에 연결된 완료 파일은 과제·제출물에 중복 연결할 수 없다")
    void rejectNoticeFileInOtherDomain(OtherDomain domain) {
        FileMetadata file = saveCompletedFile();
        patchNotice(List.of(file.getId())).then().statusCode(204);

        assertNotAttachable(attachToOtherDomain(domain, List.of(file.getId())));

        assertOtherDomainUnchanged();
        assertOnlyLinkedTo(file, "notice_attachments");
    }

    @ParameterizedTest
    @EnumSource(OtherDomain.class)
    @DisplayName("공지에서 제거한 완료 파일을 과제·제출물로 옮기면 공지에서는 다시 중복 연결할 수 없다")
    void transferRemovedNoticeFileToOtherDomain(OtherDomain domain) {
        FileMetadata file = saveCompletedFile();
        patchNotice(List.of(file.getId())).then().statusCode(204);
        patchNotice(List.of()).then().statusCode(204);
        assertCompleted(file);

        attachToOtherDomain(domain, List.of(file.getId())).then().statusCode(domain.successStatus);
        assertNotAttachable(patchNotice(List.of(file.getId())));

        assertThat(fileIds("notice_attachments", "notice_id", noticeId)).isEmpty();
        assertOnlyLinkedTo(file, domain.table);
    }

    @ParameterizedTest
    @EnumSource(OtherDomain.class)
    @DisplayName("과제·제출물에서 제거한 완료 파일을 공지에 연결할 수 있다")
    void attachFileReleasedByOtherDomain(OtherDomain domain) {
        FileMetadata file = saveCompletedFile();
        Response attached = attachToOtherDomain(domain, List.of(file.getId()));
        attached.then().statusCode(domain.successStatus);
        removeFromOtherDomain(domain, attached).then().statusCode(204);
        assertCompleted(file);

        patchNotice(List.of(file.getId())).then().statusCode(204);

        assertOnlyLinkedTo(file, "notice_attachments");
    }

    @ParameterizedTest
    @EnumSource(OtherDomain.class)
    @DisplayName("공지 수정과 과제·제출물이 같은 파일을 동시에 연결하면 하나만 성공한다")
    void preventConcurrentCrossDomainAttachment(OtherDomain domain) throws Exception {
        FileMetadata file = saveCompletedFile();
        List<Long> fileIds = List.of(file.getId());
        CyclicBarrier beforeFileLock = new CyclicBarrier(2);
        // 테스트 설정·검증에서 MANDATORY 프록시를 호출하지 않도록 내부 spy를 가져온다.
        // 실제 HTTP 요청은 원래 서비스 프록시와 DB 잠금을 그대로 거친다.
        NoticeAttachmentService noticeTarget = AopTestUtils.getUltimateTargetObject(noticeAttachmentService);
        AssignmentAttachmentService assignmentTarget = AopTestUtils.getUltimateTargetObject(assignmentAttachmentService);
        Answer<Object> awaitBothRequests = invocation -> {
            beforeFileLock.await(5, SECONDS);
            return invocation.callRealMethod();
        };
        doAnswer(awaitBothRequests).when(noticeTarget)
                .prepareForUpdate(anyLong(), any(Notice.class), eq(fileIds));
        doAnswer(awaitBothRequests).when(assignmentTarget)
                .prepare(anyLong(), anyList(), eq(fileIds));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Response noticeResponse;
        Response otherResponse;

        try {
            Future<Response> notice = executor.submit(() -> patchNotice(fileIds));
            Future<Response> other = executor.submit(() -> attachToOtherDomain(domain, fileIds));
            noticeResponse = notice.get(15, SECONDS);
            otherResponse = other.get(15, SECONDS);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }

        assertConcurrentOutcome(file, domain, noticeResponse, otherResponse);
        verify(noticeTarget).prepareForUpdate(anyLong(), any(Notice.class), eq(fileIds));
        verify(assignmentTarget).prepare(anyLong(), anyList(), eq(fileIds));
    }

    private void assertConcurrentOutcome(FileMetadata file, OtherDomain domain, Response notice, Response other) {
        if (notice.statusCode() == 204) {
            assertNotAttachable(other);
            assertOnlyLinkedTo(file, "notice_attachments");
            assertOtherDomainUnchanged();
            return;
        }
        assertNotAttachable(notice);
        other.then().statusCode(domain.successStatus);
        assertOnlyLinkedTo(file, domain.table);
        assertNoticeUnchanged();
    }

    private FileMetadata saveCompletedFile() {
        FileMetadata file = FileMetadata.create(leader, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private Response patchNotice(List<Long> fileIds) {
        return patch("/studies/" + studyId + "/notices/" + noticeId,
                Map.of("title", "수정 제목", "attachmentFileIds", fileIds));
    }

    private Response attachToOtherDomain(OtherDomain domain, List<Long> fileIds) {
        return switch (domain) {
            case ASSIGNMENT -> patch(assignmentPath(), Map.of("title", "수정 과제", "attachmentFileIds", fileIds));
            case SUBMISSION -> post(assignmentPath() + "/submissions",
                    Map.of("content", "제출 내용", "attachmentFileIds", fileIds));
        };
    }

    private Response removeFromOtherDomain(OtherDomain domain, Response attached) {
        return switch (domain) {
            case ASSIGNMENT -> patch(assignmentPath(), Map.of("attachmentFileIds", List.of()));
            case SUBMISSION -> patch(assignmentPath() + "/submissions/" + attached.jsonPath().getLong("submissionId"),
                    Map.of("attachmentFileIds", List.of()));
        };
    }

    private String assignmentPath() {
        return "/studies/" + studyId + "/assignments/" + assignmentId;
    }

    private Response post(String path, Map<String, Object> body) {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port).contentType(ContentType.JSON)
                .body(body).post(path);
    }

    private Response patch(String path, Map<String, Object> body) {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port).contentType(ContentType.JSON)
                .body(body).patch(path);
    }

    private void assertNotAttachable(Response response) {
        response.then().statusCode(400).body("code", equalTo("FILE_NOT_ATTACHABLE"));
    }

    private void assertOnlyLinkedTo(FileMetadata file, String table) {
        long total = countFileLinks("notice_attachments", file.getId())
                + countFileLinks("assignment_attachments", file.getId())
                + countFileLinks("submission_attachments", file.getId());
        assertThat(total).isOne();
        assertThat(countFileLinks(table, file.getId())).isOne();
        assertCompleted(file);
    }

    private long countFileLinks(String table, Long fileId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE file_id = ?", Long.class, fileId);
    }

    private List<Long> fileIds(String table, String parentColumn, Long parentId) {
        return jdbcTemplate.queryForList("SELECT file_id FROM " + table + " WHERE " + parentColumn
                + " = ? ORDER BY sort_order", Long.class, parentId);
    }

    private void assertCompleted(FileMetadata file) {
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    private void assertNoticeUnchanged() {
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM notices WHERE id = ?", String.class, noticeId))
                .isEqualTo("기존 제목");
        assertThat(fileIds("notice_attachments", "notice_id", noticeId)).isEmpty();
    }

    private void assertOtherDomainUnchanged() {
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM assignments WHERE id = ?", String.class, assignmentId))
                .isEqualTo("과제 제목");
        assertThat(fileIds("assignment_attachments", "assignment_id", assignmentId)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM submission_attachments a
                JOIN assignment_submissions s ON s.id = a.submission_id
                WHERE s.assignment_id = ?
                """, Long.class, assignmentId)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assignment_submissions
                WHERE assignment_id = ? AND (submitted_at IS NOT NULL OR content IS NOT NULL)
                """, Long.class, assignmentId)).isZero();
    }

    private enum OtherDomain {
        ASSIGNMENT("assignment_attachments", 204),
        SUBMISSION("submission_attachments", 201);

        private final String table;
        private final int successStatus;

        OtherDomain(String table, int successStatus) {
            this.table = table;
            this.successStatus = successStatus;
        }
    }
}
