package withoutc.chongchong.notice;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verifyNoInteractions;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.NotificationRepository;
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
        "notice.attachments.max-count=1",
        "notice.attachments.max-size-bytes=1",
        "notice.attachments.allowed-content-types=image/jpeg"
})
class NoticeDeleteAttachmentApiTest extends PostgresContainerTest {

    private static final LocalDateTime REMIND_AT = LocalDateTime.of(2099, 1, 1, 0, 0);
    private static final LocalDateTime READ_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private S3FileMetadataReader s3FileMetadataReader;

    @LocalServerPort
    private int port;

    private User leader;
    private User member;
    private User previousLeader;
    private Study study;
    private Notice notice;
    private Notice otherNotice;
    private List<Long> fileIds;
    private Long otherFileId;
    private Long pendingFileId;
    private List<Long> originalAttachmentIds;
    private List<Long> originalNotificationIds;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("현재 리더", null));
        member = userRepository.save(User.create("스터디원", null));
        previousLeader = userRepository.save(User.create("이전 리더", null));
        study = studyRepository.save(Study.create("공지 삭제 HTTP", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        StudyMember studyMember = studyMemberRepository.save(
                StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        FileMetadata first = saveCompletedFile("first.png");
        FileMetadata second = saveCompletedFile("second.png");
        FileMetadata otherFile = saveCompletedFile("other.png");
        pendingFileId = fileRepository.saveAndFlush(FileMetadata.create(
                leader, "pending.png", "uploads/pending.png", "image/png", 1024)).getId();
        fileIds = List.of(second.getId(), first.getId());
        otherFileId = otherFile.getId();
        notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addAttachments(List.of(second, first));
        notice.addReminders(List.of(REMIND_AT), READ_AT);
        notice.addRecipients(List.of(studyMember));
        notice.getRecipients().getFirst().markAsRead(READ_AT);
        noticeRepository.saveAndFlush(notice);
        otherNotice = Notice.create(study, "다른 공지", "다른 내용");
        otherNotice.addAttachments(List.of(otherFile));
        noticeRepository.saveAndFlush(otherNotice);
        notificationRepository.saveAllAndFlush(List.of(
                notification(notice.getId(), NotificationType.NEW),
                notification(notice.getId(), NotificationType.REMIND),
                notification(otherNotice.getId(), NotificationType.NEW)));
        originalAttachmentIds = jdbcTemplate.queryForList(
                "SELECT id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order", Long.class, notice.getId());
        originalNotificationIds = notificationRepository.findAll().stream().map(Notification::getId).sorted().toList();
    }

    @AfterEach
    void cleanDatabase() {
        verifyNoInteractions(s3FileMetadataReader);
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("현재 정책을 벗어난 이전 리더 첨부의 관계만 삭제하고 완료 파일을 보존하며 204를 반환한다")
    void deleteAttachmentsWithoutStorageVerification() {
        Response response = request(leader.getId(), notice.getId());

        response.then().statusCode(204);
        assertThat(response.asString()).isEmpty();
        assertDeleted();
        assertThat(fileRepository.findAll()).hasSize(4).allSatisfy(file ->
                assertThat(file.getObjectKey()).isEqualTo("uploads/" + file.getOriginalFileName()));
        testAuthRequest.givenAuthenticatedUser(member.getId()).port(port)
                .get("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then().statusCode(404).body("code", equalTo("NOTICE_NOT_FOUND"));
    }

    @Test
    @DisplayName("첨부 없는 공지 삭제는 다른 공지와 파일에 영향을 주지 않는다")
    void deleteNoticeWithoutAttachments() {
        Notice emptyNotice = noticeRepository.saveAndFlush(Notice.create(study, "무첨부 공지", "내용"));

        request(leader.getId(), emptyNotice.getId()).then().statusCode(204);

        assertThat(noticeRepository.existsById(emptyNotice.getId())).isFalse();
        assertOriginalState();
    }

    @Test
    @DisplayName("일반 스터디원의 삭제는 403 ACCESS_DENIED를 반환하고 기존 데이터를 유지한다")
    void rejectMemberDeletion() {
        assertRejected(request(member.getId(), notice.getId()), 403, "ACCESS_DENIED");
    }

    @Test
    @DisplayName("파일 소유자여도 현재 스터디원이 아니면 삭제 시 403 STUDY_ACCESS_DENIED를 반환한다")
    void rejectFormerLeadersDeletion() {
        assertRejected(request(previousLeader.getId(), notice.getId()), 403, "STUDY_ACCESS_DENIED");
    }

    @Test
    @DisplayName("인증 없는 삭제는 401 AUTHENTICATION_REQUIRED를 반환한다")
    void rejectAnonymousDeletion() {
        Response response = given().port(port).basePath(API_PREFIX)
                .delete("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId());

        assertRejected(response, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("존재하지 않는 공지의 삭제는 404 NOTICE_NOT_FOUND를 반환한다")
    void rejectMissingNotice() {
        assertRejected(request(leader.getId(), Long.MAX_VALUE), 404, "NOTICE_NOT_FOUND");
    }

    @Test
    @DisplayName("다른 스터디의 공지 삭제는 404 NOTICE_NOT_FOUND를 반환한다")
    void rejectNoticeFromOtherStudy() {
        Study otherStudy = studyRepository.save(Study.create("다른 스터디", "설명"));
        Notice foreignNotice = noticeRepository.saveAndFlush(Notice.create(otherStudy, "다른 제목", "다른 내용"));

        assertRejected(request(leader.getId(), foreignNotice.getId()), 404, "NOTICE_NOT_FOUND");
        assertThat(noticeRepository.existsById(foreignNotice.getId())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "1.5"})
    @DisplayName("공지 ID의 형식이 잘못되면 400 INVALID_REQUEST를 반환한다")
    void rejectMalformedNoticeId(String noticeId) {
        assertRejected(request(leader.getId(), noticeId), 400, "INVALID_REQUEST");
    }

    @Test
    @DisplayName("스터디 ID의 형식이 잘못되면 400 INVALID_REQUEST를 반환한다")
    void rejectMalformedStudyId() {
        Response response = testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .delete("/studies/{studyId}/notices/{noticeId}", "invalid", notice.getId());

        assertRejected(response, 400, "INVALID_REQUEST");
    }

    @Test
    @DisplayName("이미 삭제한 공지를 다시 삭제하면 404를 반환하고 파일의 완료 상태를 유지한다")
    void rejectRepeatedDeletion() {
        request(leader.getId(), notice.getId()).then().statusCode(204);

        request(leader.getId(), notice.getId()).then().statusCode(404)
                .body("code", equalTo("NOTICE_NOT_FOUND"));

        assertDeleted();
    }

    private Response request(Long userId, Object noticeId) {
        return testAuthRequest.givenAuthenticatedUser(userId).port(port)
                .delete("/studies/{studyId}/notices/{noticeId}", study.getId(), noticeId);
    }

    private FileMetadata saveCompletedFile(String name) {
        FileMetadata file = FileMetadata.create(previousLeader, name, "uploads/" + name, "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private Notification notification(Long noticeId, NotificationType type) {
        return Notification.create(member, "공지 알림", "내용", type, noticeId, ResourceType.NOTICE, "/notices/" + noticeId);
    }

    private void assertStatus(Long fileId, FileUploadStatus status) {
        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(status);
    }

    private void assertRejected(Response response, int status, String code) {
        response.then().statusCode(status).body("code", equalTo(code));
        assertOriginalState();
    }

    private void assertOriginalState() {
        Notice found = noticeRepository.findById(notice.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("기존 제목");
        assertThat(found.getContent()).isEqualTo("기존 내용");
        assertThat(jdbcTemplate.queryForList("SELECT id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order",
                Long.class, notice.getId())).containsExactlyElementsOf(originalAttachmentIds);
        assertThat(jdbcTemplate.queryForList("SELECT remind_at FROM notice_reminders WHERE notice_id = ?",
                LocalDateTime.class, notice.getId())).containsExactly(REMIND_AT);
        assertThat(jdbcTemplate.queryForList("SELECT read_at FROM notice_recipients WHERE notice_id = ?",
                LocalDateTime.class, notice.getId())).containsExactly(READ_AT);
        assertThat(notificationRepository.findAll().stream().map(Notification::getId).sorted().toList())
                .containsExactlyElementsOf(originalNotificationIds);
        assertStatus(fileIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(fileIds.getLast(), FileUploadStatus.COMPLETED);
        assertStatus(pendingFileId, FileUploadStatus.PENDING);
        assertOtherNoticeUnchanged();
    }

    private void assertDeleted() {
        assertThat(noticeRepository.existsById(notice.getId())).isFalse();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notice_reminders")).isZero();
        assertThat(countRows("notice_recipients")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE resource_type = 'NOTICE' AND resource_id = ?",
                Long.class, notice.getId())).isZero();
        assertStatus(fileIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(fileIds.getLast(), FileUploadStatus.COMPLETED);
        assertStatus(pendingFileId, FileUploadStatus.PENDING);
        assertOtherNoticeUnchanged();
    }

    private long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE notice_id = ?",
                Long.class, notice.getId());
    }

    private void assertOtherNoticeUnchanged() {
        assertThat(noticeRepository.findById(otherNotice.getId()).orElseThrow().getTitle()).isEqualTo("다른 공지");
        assertThat(jdbcTemplate.queryForList("SELECT file_id FROM notice_attachments WHERE notice_id = ?",
                Long.class, otherNotice.getId())).containsExactly(otherFileId);
        assertStatus(otherFileId, FileUploadStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE resource_type = 'NOTICE' AND resource_id = ?",
                Long.class, otherNotice.getId())).isOne();
    }
}
