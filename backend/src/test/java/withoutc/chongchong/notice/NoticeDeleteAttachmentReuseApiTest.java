package withoutc.chongchong.notice;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notice.service.NoticeService;
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
        "file.upload.max-size-bytes=5242880",
        "notice.attachments.max-count=10",
        "notice.attachments.max-size-bytes=5242880",
        "notice.attachments.allowed-content-types=image/jpeg,image/png,image/webp"
})
class NoticeDeleteAttachmentReuseApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @MockitoBean
    private S3FileMetadataReader metadataReader;

    @LocalServerPort
    private int port;

    private User leader;
    private Long studyId;
    private Long deletedNoticeId;
    private Long targetNoticeId;
    private Long assignmentId;
    private FileMetadata file;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("리더", null));
        User member = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("삭제 파일 재사용 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        studyId = study.getId();
        file = fileRepository.saveAndFlush(FileMetadata.create(
                leader, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024));
        when(metadataReader.findSize(file.getObjectKey())).thenReturn(OptionalLong.of(file.getSize()));
        testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .post("/files/{fileId}/complete", file.getId()).then().statusCode(204);
        Response notice = post(noticesPath(), Map.of(
                "title", "삭제할 공지", "content", "기존 내용", "remindAts", List.of("2099-01-01T00:00:00"),
                "attachmentFileIds", List.of(file.getId())));
        notice.then().statusCode(201);
        deletedNoticeId = notice.jsonPath().getLong("noticeId");
        Response target = post(noticesPath(), Map.of("title", "대상 공지", "content", "대상 내용"));
        target.then().statusCode(201);
        targetNoticeId = target.jsonPath().getLong("noticeId");
        Response assignment = post("/studies/" + studyId + "/assignments", Map.of(
                "title", "과제 제목", "content", "과제 내용", "submissionTarget", "MEMBERS_AND_LEADER",
                "submissionVisibility", "LEADER_ONLY", "closeAt", "2099-12-31T23:59:00"));
        assignment.then().statusCode(201);
        assignmentId = assignment.jsonPath().getLong("assignmentId");
    }

    @AfterEach
    void cleanDatabase() {
        verify(metadataReader).findSize(file.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
        databaseCleaner.clean();
    }

    @ParameterizedTest
    @EnumSource(AttachmentTarget.class)
    @DisplayName("삭제 전에는 중복 첨부를 거부하고 공지 삭제 후에는 완료 API 재호출 없이 같은 파일을 재사용한다")
    void reuseFileAfterDeletingNotice(AttachmentTarget target) {
        assertNotAttachable(attach(target));
        assertOriginalNotice();
        assertTargetUnchanged();

        deleteNotice().then().statusCode(204);
        assertDeletedNotice();
        assertThat(totalFileLinks()).isZero();
        assertFilePreserved();

        Response response = attach(target);

        response.then().statusCode(target.successStatus);
        assertOnlyLinkedToTarget(target, response);
        // 이미 삭제된 공지의 재삭제 실패가 새 연결에 영향을 주면 안 된다.
        deleteNotice().then().statusCode(404).body("code", equalTo("NOTICE_NOT_FOUND"));
        assertOnlyLinkedToTarget(target, response);
    }

    @ParameterizedTest
    @EnumSource(AttachmentTarget.class)
    @DisplayName("공지 삭제 SQL 이후 잠금을 기다린 첨부 요청은 삭제 커밋 후 파일을 재사용한다")
    void reuseFileAfterConcurrentDeletionCommits(AttachmentTarget target) throws Exception {
        Response response = attachWhileDeleting(target, false);

        response.then().statusCode(target.successStatus);
        assertDeletedNotice();
        assertOnlyLinkedToTarget(target, response);
    }

    @ParameterizedTest
    @EnumSource(AttachmentTarget.class)
    @DisplayName("공지 삭제가 롤백되면 대기 중인 첨부 요청은 복구된 기존 연결을 확인해 거부된다")
    void rejectReuseAfterConcurrentDeletionRollsBack(AttachmentTarget target) throws Exception {
        Response response = attachWhileDeleting(target, true);

        assertNotAttachable(response);
        assertOriginalNotice();
        assertTargetUnchanged();
        assertThat(totalFileLinks()).isOne();
        assertFilePreserved();
    }

    private Response attachWhileDeleting(AttachmentTarget target, boolean rollback) throws Exception {
        CountDownLatch deletionFlushed = new CountDownLatch(1);
        CountDownLatch releaseDeletion = new CountDownLatch(1);
        AtomicInteger deletionPid = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> deletion = executor.submit(() ->
                    deleteWithinHeldTransaction(rollback, deletionFlushed, releaseDeletion, deletionPid));
            await(deletionFlushed);
            Future<Response> attachment = executor.submit(() -> attach(target));
            awaitFileLockWait(deletionPid.get());
            assertThat(attachment.isDone()).isFalse();
            releaseDeletion.countDown();
            deletion.get(15, SECONDS);
            return attachment.get(15, SECONDS);
        } finally {
            releaseDeletion.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }
    }

    private void deleteWithinHeldTransaction(boolean rollback, CountDownLatch flushed, CountDownLatch release,
                                            AtomicInteger deletionPid) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            noticeService.delete(leader.getId(), studyId, deletedNoticeId);
            entityManager.flush();
            deletionPid.set(jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
            flushed.countDown();
            await(release);
            if (rollback) {
                status.setRollbackOnly();
            }
        });
    }

    private void awaitFileLockWait(int deletionPid) throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        while (!hasFileLockWaiter(deletionPid) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(hasFileLockWaiter(deletionPid))
                .as("새 첨부 요청이 공지 삭제 트랜잭션의 파일 행 잠금을 기다려야 한다").isTrue();
    }

    private boolean hasFileLockWaiter(int deletionPid) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND wait_event_type = 'Lock'
                      AND ? = ANY(pg_blocking_pids(pid))
                      AND query ILIKE '%file_metadata%'
                )
                """, Boolean.class, deletionPid));
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, SECONDS)).as("삭제 트랜잭션 동기화 대기").isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("삭제 트랜잭션 대기 중 인터럽트 발생", exception);
        }
    }

    private Response attach(AttachmentTarget target) {
        List<Long> fileIds = List.of(file.getId());
        return switch (target) {
            case NOTICE_CREATE -> post(noticesPath(), Map.of(
                    "title", "재사용 공지", "content", "새 내용", "attachmentFileIds", fileIds));
            case NOTICE_UPDATE -> patch(noticesPath() + "/" + targetNoticeId,
                    Map.of("title", "수정 공지", "attachmentFileIds", fileIds));
            case ASSIGNMENT -> patch(assignmentPath(), Map.of("title", "수정 과제", "attachmentFileIds", fileIds));
            case SUBMISSION -> post(assignmentPath() + "/submissions",
                    Map.of("content", "제출 내용", "attachmentFileIds", fileIds));
        };
    }

    private Response deleteNotice() {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .delete(noticesPath() + "/" + deletedNoticeId);
    }

    private Response post(String path, Map<String, Object> body) {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port).contentType(ContentType.JSON)
                .body(body).post(path);
    }

    private Response patch(String path, Map<String, Object> body) {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port).contentType(ContentType.JSON)
                .body(body).patch(path);
    }

    private String noticesPath() {
        return "/studies/" + studyId + "/notices";
    }

    private String assignmentPath() {
        return "/studies/" + studyId + "/assignments/" + assignmentId;
    }

    private void assertNotAttachable(Response response) {
        response.then().statusCode(400).body("code", equalTo("FILE_NOT_ATTACHABLE"));
    }

    private void assertOnlyLinkedToTarget(AttachmentTarget target, Response response) {
        Long parentId = switch (target) {
            case NOTICE_CREATE -> response.jsonPath().getLong("noticeId");
            case NOTICE_UPDATE -> targetNoticeId;
            case ASSIGNMENT -> assignmentId;
            case SUBMISSION -> response.jsonPath().getLong("submissionId");
        };
        assertThat(totalFileLinks()).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + target.table
                + " WHERE file_id = ? AND " + target.parentColumn + " = ?",
                Long.class, file.getId(), parentId)).isOne();
        assertFilePreserved();
    }

    private long totalFileLinks() {
        return countFileLinks("notice_attachments") + countFileLinks("assignment_attachments")
                + countFileLinks("submission_attachments");
    }

    private long countFileLinks(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE file_id = ?",
                Long.class, file.getId());
    }

    private void assertFilePreserved() {
        FileMetadata found = fileRepository.findById(file.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(found.getObjectKey()).isEqualTo(file.getObjectKey());
        assertThat(found.getSize()).isEqualTo(file.getSize());
        assertThat(found.isOwnedBy(leader.getId())).isTrue();
    }

    private void assertDeletedNotice() {
        testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .get(noticesPath() + "/" + deletedNoticeId).then().statusCode(404)
                .body("code", equalTo("NOTICE_NOT_FOUND"));
        assertThat(noticeRowCount("notice_attachments")).isZero();
        assertThat(noticeRowCount("notice_reminders")).isZero();
        assertThat(noticeRowCount("notice_recipients")).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notifications WHERE resource_type = 'NOTICE' AND resource_id = ?
                """, Long.class, deletedNoticeId)).isZero();
    }

    private void assertOriginalNotice() {
        testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .get(noticesPath() + "/" + deletedNoticeId).then().statusCode(200)
                .body("title", equalTo("삭제할 공지"));
        assertThat(jdbcTemplate.queryForList("SELECT file_id FROM notice_attachments WHERE notice_id = ?",
                Long.class, deletedNoticeId)).containsExactly(file.getId());
        assertThat(noticeRowCount("notice_reminders")).isOne();
        assertThat(noticeRowCount("notice_recipients")).isOne();
    }

    private long noticeRowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE notice_id = ?",
                Long.class, deletedNoticeId);
    }

    private void assertTargetUnchanged() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notices", Long.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM notices WHERE id = ?", String.class, targetNoticeId))
                .isEqualTo("대상 공지");
        assertThat(jdbcTemplate.queryForList("SELECT file_id FROM notice_attachments WHERE notice_id = ?",
                Long.class, targetNoticeId)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM assignments WHERE id = ?", String.class, assignmentId))
                .isEqualTo("과제 제목");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM assignment_submissions
                WHERE assignment_id = ? AND (submitted_at IS NOT NULL OR content IS NOT NULL)
                """, Long.class, assignmentId)).isZero();
    }

    private enum AttachmentTarget {
        NOTICE_CREATE("notice_attachments", "notice_id", 201),
        NOTICE_UPDATE("notice_attachments", "notice_id", 204),
        ASSIGNMENT("assignment_attachments", "assignment_id", 204),
        SUBMISSION("submission_attachments", "submission_id", 201);

        private final String table;
        private final String parentColumn;
        private final int successStatus;

        AttachmentTarget(String table, String parentColumn, int successStatus) {
            this.table = table;
            this.parentColumn = parentColumn;
            this.successStatus = successStatus;
        }
    }
}
