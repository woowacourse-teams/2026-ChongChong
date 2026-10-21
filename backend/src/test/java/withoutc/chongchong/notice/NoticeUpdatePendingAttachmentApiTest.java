package withoutc.chongchong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
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
class NoticeUpdatePendingAttachmentApiTest extends PostgresContainerTest {

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
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private Long leaderId;
    private Long studyId;
    private Long noticeId;
    private List<Long> existingIds;
    private List<Long> originalAttachmentIds;
    private Long newFileId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        User leader = userRepository.save(User.create("현재 리더", null));
        User previousLeader = userRepository.save(User.create("이전 리더", null));
        Study study = studyRepository.save(Study.create("검증 대기 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        FileMetadata first = saveCompletedFile(previousLeader, "first.png");
        FileMetadata second = saveCompletedFile(previousLeader, "second.png");
        FileMetadata added = fileRepository.saveAndFlush(FileMetadata.create(
                leader, "new.png", "uploads/new.png", "image/png", 1024));
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addAttachments(List.of(first, second));
        notice.addReminders(List.of(LocalDateTime.of(2099, 1, 1, 0, 0)), LocalDateTime.of(2026, 1, 1, 0, 0));
        noticeRepository.saveAndFlush(notice);
        leaderId = leader.getId();
        studyId = study.getId();
        noticeId = notice.getId();
        existingIds = List.of(first.getId(), second.getId());
        newFileId = added.getId();
        originalAttachmentIds = jdbcTemplate.queryForList(
                "SELECT id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order", Long.class, noticeId);
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("기존 첨부를 유지하더라도 미완료 파일 추가는 400으로 거부하고 전체 수정을 롤백한다")
    void rejectPendingNewFile() {
        Response response = request(Map.of("title", "수정 제목", "content", "수정 내용",
                "remindAts", List.of("2099-01-02T00:00:00"),
                "attachmentFileIds", List.of(existingIds.getFirst(), newFileId)));

        response.then().statusCode(400)
                .body("code", equalTo("FILE_NOT_ATTACHABLE"))
                .body("message", equalTo("파일을 첨부할 수 없습니다."));
        Notice found = noticeRepository.findById(noticeId).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("기존 제목");
        assertThat(found.getContent()).isEqualTo("기존 내용");
        assertThat(jdbcTemplate.queryForList("SELECT id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order",
                Long.class, noticeId)).containsExactlyElementsOf(originalAttachmentIds);
        assertThat(attachmentFileIds()).containsExactlyElementsOf(existingIds);
        assertThat(jdbcTemplate.queryForList("SELECT remind_at FROM notice_reminders WHERE notice_id = ?",
                LocalDateTime.class, noticeId)).containsExactly(LocalDateTime.of(2099, 1, 1, 0, 0));
        assertStatus(existingIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getLast(), FileUploadStatus.COMPLETED);
        assertStatus(newFileId, FileUploadStatus.PENDING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"title\":\"수정 제목\"}", "{\"title\":\"수정 제목\",\"attachmentFileIds\":null}"})
    @DisplayName("미완료 파일과 무관하게 첨부 생략·null로 기존 첨부를 유지하며 수정할 수 있다")
    void preserveExistingAttachments(String body) {
        request(body).then().statusCode(204);

        assertThat(noticeRepository.findById(noticeId).orElseThrow().getTitle()).isEqualTo("수정 제목");
        assertThat(attachmentFileIds()).containsExactlyElementsOf(existingIds);
        assertStatus(existingIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getLast(), FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("미완료 파일과 무관하게 이전 리더의 기존 첨부를 재정렬할 수 있다")
    void reorderExistingAttachments() {
        request(Map.of("attachmentFileIds", existingIds.reversed())).then().statusCode(204);

        assertThat(attachmentFileIds()).containsExactlyElementsOf(existingIds.reversed());
        assertStatus(existingIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getLast(), FileUploadStatus.COMPLETED);
        assertStatus(newFileId, FileUploadStatus.PENDING);
    }

    @Test
    @DisplayName("미완료 파일과 무관하게 기존 첨부 일부를 제거하고 남은 파일은 유지할 수 있다")
    void removeOneExistingAttachment() {
        request(Map.of("attachmentFileIds", List.of(existingIds.getFirst()))).then().statusCode(204);

        assertThat(attachmentFileIds()).containsExactly(existingIds.getFirst());
        assertStatus(existingIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getLast(), FileUploadStatus.DELETE_PENDING);
        assertStatus(newFileId, FileUploadStatus.PENDING);
    }

    @Test
    @DisplayName("미완료 파일과 무관하게 빈 목록으로 첨부를 모두 제거할 수 있다")
    void removeAllExistingAttachments() {
        request(Map.of("attachmentFileIds", List.of())).then().statusCode(204);

        assertThat(attachmentFileIds()).isEmpty();
        assertStatus(existingIds.getFirst(), FileUploadStatus.DELETE_PENDING);
        assertStatus(existingIds.getLast(), FileUploadStatus.DELETE_PENDING);
        assertStatus(newFileId, FileUploadStatus.PENDING);
    }

    private FileMetadata saveCompletedFile(User owner, String name) {
        FileMetadata file = FileMetadata.create(owner, name, "uploads/" + name, "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private Response request(Object body) {
        return testAuthRequest.givenAuthenticatedUser(leaderId).port(port).contentType(ContentType.JSON)
                .body(body).patch("/studies/{studyId}/notices/{noticeId}", studyId, noticeId);
    }

    private List<Long> attachmentFileIds() {
        return jdbcTemplate.queryForList("SELECT file_id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order",
                Long.class, noticeId);
    }

    private void assertStatus(Long id, FileUploadStatus status) {
        assertThat(fileRepository.findById(id).orElseThrow().getStatus()).isEqualTo(status);
    }
}
