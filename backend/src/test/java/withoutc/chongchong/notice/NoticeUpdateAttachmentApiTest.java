package withoutc.chongchong.notice;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class NoticeUpdateAttachmentApiTest extends PostgresContainerTest {

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
    private User outsider;
    private Study study;
    private Notice notice;
    private List<Long> existingIds;
    private List<Long> originalAttachmentIds;
    private List<Long> originalReminderIds;
    private FileMetadata firstNewFile;
    private FileMetadata secondNewFile;
    private FileMetadata memberFile;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("리더", null));
        member = userRepository.save(User.create("스터디원", null));
        outsider = userRepository.save(User.create("외부 사용자", null));
        study = studyRepository.save(Study.create("공지 수정 HTTP 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        StudyMember studyMember = studyMemberRepository.save(
                StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        FileMetadata first = saveCompletedFile();
        FileMetadata second = saveCompletedFile();
        firstNewFile = saveFile(leader, "image/jpeg", 5242880);
        secondNewFile = saveFile(leader, "image/webp", 1024);
        memberFile = saveFile(member, "image/png", 1024);
        notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addAttachments(List.of(first, second));
        notice.addReminders(List.of(REMIND_AT), READ_AT);
        notice.addRecipients(List.of(studyMember));
        notice.getRecipients().getFirst().markAsRead(READ_AT);
        noticeRepository.saveAndFlush(notice);
        existingIds = List.of(first.getId(), second.getId());
        originalAttachmentIds = attachmentIds();
        originalReminderIds = reminderIds();
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("첨부 교체는 본문 없는 204를 반환하며 상세 조회에도 최종 첨부 순서가 반영된다")
    void replaceAttachmentsAndReadUpdatedDetail() {
        List<Long> finalIds = List.of(firstNewFile.getId(), existingIds.getFirst());

        Response response = request(leader.getId(), validBody(finalIds));

        response.then().statusCode(204);
        assertThat(response.asString()).isEmpty();
        testAuthRequest.givenAuthenticatedUser(member.getId()).port(port)
                .get("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then().statusCode(200)
                .body("title", equalTo("수정 제목"))
                .body("content", equalTo("수정 내용"))
                .body("attachments.fileId", contains(firstNewFile.getId().intValue(), existingIds.getFirst().intValue()))
                .body("attachments.sortOrder", contains(0, 1));
        assertStatus(firstNewFile.getId(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getLast(), FileUploadStatus.COMPLETED);
        assertStatus(secondNewFile.getId(), FileUploadStatus.COMPLETED);
        assertThat(remindAt()).isEqualTo(REMIND_AT.plusDays(1));
        assertReadStateUnchanged();
        verifyNoInteractions(s3FileMetadataReader);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"title\":\"수정 제목\"}", "{\"title\":\"수정 제목\",\"attachmentFileIds\":null}"})
    @DisplayName("첨부 필드 생략과 null은 기존 첨부 행과 파일 상태를 유지한다")
    void preserveAttachments(String body) {
        request(leader.getId(), body).then().statusCode(204);

        assertThat(noticeRepository.findById(notice.getId()).orElseThrow().getTitle()).isEqualTo("수정 제목");
        assertAttachmentsUnchanged();
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("빈 첨부 목록으로 수정하면 전체 제거되고 상세 조회는 빈 배열을 반환한다")
    void removeAllAttachments() {
        request(leader.getId(), Map.of("attachmentFileIds", List.of())).then().statusCode(204);

        testAuthRequest.givenAuthenticatedUser(member.getId()).port(port)
                .get("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then().statusCode(200).body("attachments", hasSize(0));
        assertStatus(existingIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getLast(), FileUploadStatus.COMPLETED);
        assertReadStateUnchanged();
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("기존 2장과 새 이미지 8장을 합쳐 최대 10장까지 장당 5MiB로 수정할 수 있다")
    void acceptMaximumFinalCountAndNewFileSize() {
        List<Long> addedIds = LongStream.range(0, 8)
                .mapToObj(index -> saveFile(leader, "image/png", 5242880).getId()).toList();
        List<Long> finalIds = Stream.concat(existingIds.stream(), addedIds.stream()).toList();

        request(leader.getId(), validBody(finalIds)).then().statusCode(204);

        assertThat(attachmentFileIds()).containsExactlyElementsOf(finalIds);
        assertThat(fileRepository.findAllById(finalIds)).hasSize(10).extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.COMPLETED);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("최종 첨부가 개수 제한을 넘으면 400 ATTACHMENT_LIMIT_EXCEEDED를 반환한다")
    void rejectTooManyAttachments() {
        List<Long> ids = LongStream.rangeClosed(1, 11).boxed().toList();

        assertRejected(request(leader.getId(), validBody(ids)), 400, "ATTACHMENT_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("기존 첨부 ID도 중복되면 400 INVALID_ATTACHMENT_IDS를 반환한다")
    void rejectDuplicateIds() {
        assertRejected(request(leader.getId(), validBody(List.of(existingIds.getFirst(), existingIds.getFirst()))),
                400, "INVALID_ATTACHMENT_IDS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[null]", "[0]", "[-1]"})
    @DisplayName("null·0·음수 ID는 400 INVALID_INPUT_VALUE를 반환한다")
    void rejectInvalidId(String idsJson) {
        Response response = request(leader.getId(), "{\"title\":\"수정 제목\",\"attachmentFileIds\":%s}".formatted(idsJson));

        assertRejected(response, 400, "INVALID_INPUT_VALUE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"invalid\"", "[\"invalid\"]"})
    @DisplayName("첨부 ID의 JSON 타입이 잘못되면 400 INVALID_REQUEST를 반환한다")
    void rejectInvalidJsonType(String idsJson) {
        Response response = request(leader.getId(), "{\"title\":\"수정 제목\",\"attachmentFileIds\":%s}".formatted(idsJson));

        assertRejected(response, 400, "INVALID_REQUEST");
    }

    @Test
    @DisplayName("추가할 파일이 없으면 404 FILE_NOT_FOUND를 반환한다")
    void rejectMissingFile() {
        assertRejected(request(leader.getId(), validBody(List.of(Long.MAX_VALUE))), 404, "FILE_NOT_FOUND");
    }

    @Test
    @DisplayName("타인 소유의 새 파일을 추가하면 403 FILE_NOT_OWNED를 반환한다")
    void rejectOtherUsersNewFile() {
        assertRejected(request(leader.getId(), validBody(List.of(memberFile.getId()))), 403, "FILE_NOT_OWNED");
    }

    @ParameterizedTest
    @EnumSource(value = FileUploadStatus.class, names = {"PENDING", "DELETE_PENDING"})
    @DisplayName("미완료·삭제 대기 파일은 400 FILE_NOT_ATTACHABLE을 반환한다")
    void rejectNonCompletedNewFile(FileUploadStatus status) {
        FileMetadata file = FileMetadata.create(leader, "used.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        if (status == FileUploadStatus.DELETE_PENDING) {
            file.completeUpload();
            file.markForDeletion();
        }
        fileRepository.saveAndFlush(file);

        assertRejected(request(leader.getId(), validBody(List.of(file.getId()))), 400, "FILE_NOT_ATTACHABLE");
        assertStatus(file.getId(), status);
    }

    @Test
    @DisplayName("새 파일의 형식이 허용되지 않으면 400 INVALID_NOTICE_IMAGE_TYPE을 반환한다")
    void rejectUnsupportedImageType() {
        FileMetadata file = saveFile(leader, "application/pdf", 1024);

        assertRejected(request(leader.getId(), validBody(List.of(file.getId()))), 400, "INVALID_NOTICE_IMAGE_TYPE");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5242881L})
    @DisplayName("빈 이미지와 용량 초과 이미지는 400 INVALID_NOTICE_IMAGE_SIZE를 반환한다")
    void rejectInvalidImageSize(long size) {
        FileMetadata file = saveFile(leader, "image/png", size);

        assertRejected(request(leader.getId(), validBody(List.of(file.getId()))), 400, "INVALID_NOTICE_IMAGE_SIZE");
    }

    @Test
    @DisplayName("일반 스터디원은 본인 파일을 첨부해도 403 ACCESS_DENIED를 반환한다")
    void rejectMemberUpdate() {
        assertRejected(request(member.getId(), validBody(List.of(memberFile.getId()))), 403, "ACCESS_DENIED");
    }

    @Test
    @DisplayName("외부 사용자의 수정은 403 STUDY_ACCESS_DENIED를 반환한다")
    void rejectOutsiderUpdate() {
        assertRejected(request(outsider.getId(), validBody(existingIds)), 403, "STUDY_ACCESS_DENIED");
    }

    @Test
    @DisplayName("인증 없는 수정은 401 AUTHENTICATION_REQUIRED를 반환한다")
    void rejectAnonymousUpdate() {
        Response response = given().port(port).basePath(API_PREFIX).contentType(ContentType.JSON)
                .body(validBody(existingIds)).patch("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId());

        assertRejected(response, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("존재하지 않는 공지의 수정은 404 NOTICE_NOT_FOUND를 반환한다")
    void rejectMissingNotice() {
        assertRejected(request(leader.getId(), Long.MAX_VALUE, validBody(existingIds)), 404, "NOTICE_NOT_FOUND");
    }

    @Test
    @DisplayName("다른 스터디의 공지 수정은 404 NOTICE_NOT_FOUND를 반환한다")
    void rejectNoticeFromOtherStudy() {
        Study otherStudy = studyRepository.save(Study.create("다른 스터디", "설명"));
        Notice otherNotice = noticeRepository.saveAndFlush(Notice.create(otherStudy, "다른 제목", "다른 내용"));

        assertRejected(request(leader.getId(), otherNotice.getId(), validBody(existingIds)), 404, "NOTICE_NOT_FOUND");
        assertThat(noticeRepository.findById(otherNotice.getId()).orElseThrow().getTitle()).isEqualTo("다른 제목");
    }

    @Test
    @DisplayName("일부 이미지 정책 검증 실패는 400을 반환하고 수정 내용과 완료 상태를 유지한다")
    void rejectPartialImagePolicyFailure() {
        jdbcTemplate.update("UPDATE file_metadata SET content_type = 'application/pdf' WHERE id = ?", secondNewFile.getId());

        Response response = request(leader.getId(), validBody(List.of(firstNewFile.getId(), secondNewFile.getId())));

        response.then().statusCode(400).body("code", equalTo("INVALID_NOTICE_IMAGE_TYPE"));
        assertOriginalState();
        verifyNoInteractions(s3FileMetadataReader);
    }

    private Response request(Long userId, Object body) {
        return request(userId, notice.getId(), body);
    }

    private Response request(Long userId, Long noticeId, Object body) {
        return testAuthRequest.givenAuthenticatedUser(userId).port(port).contentType(ContentType.JSON)
                .body(body).patch("/studies/{studyId}/notices/{noticeId}", study.getId(), noticeId);
    }

    private Map<String, Object> validBody(List<Long> ids) {
        return Map.of("title", "수정 제목", "content", "수정 내용", "remindAts", List.of("2099-01-02T00:00:00"),
                "attachmentFileIds", ids);
    }

    private FileMetadata saveFile(User owner, String type, long size) {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/" + UUID.randomUUID(), type, size);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private FileMetadata saveCompletedFile() {
        FileMetadata file = FileMetadata.create(leader, "existing.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private List<Long> attachmentFileIds() {
        return jdbcTemplate.queryForList("SELECT file_id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order",
                Long.class, notice.getId());
    }

    private List<Long> attachmentIds() {
        return jdbcTemplate.queryForList("SELECT id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order",
                Long.class, notice.getId());
    }

    private List<Long> reminderIds() {
        return jdbcTemplate.queryForList("SELECT id FROM notice_reminders WHERE notice_id = ?", Long.class, notice.getId());
    }

    private LocalDateTime remindAt() {
        return jdbcTemplate.queryForObject("SELECT remind_at FROM notice_reminders WHERE notice_id = ?",
                LocalDateTime.class, notice.getId());
    }

    private void assertStatus(Long id, FileUploadStatus status) {
        assertThat(fileRepository.findById(id).orElseThrow().getStatus()).isEqualTo(status);
    }

    private void assertRejected(Response response, int status, String code) {
        response.then().statusCode(status).body("code", equalTo(code));
        assertOriginalState();
        verifyNoInteractions(s3FileMetadataReader);
    }

    private void assertOriginalState() {
        Notice found = noticeRepository.findById(notice.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("기존 제목");
        assertThat(found.getContent()).isEqualTo("기존 내용");
        assertAttachmentsUnchanged();
    }

    private void assertAttachmentsUnchanged() {
        assertThat(attachmentIds()).containsExactlyElementsOf(originalAttachmentIds);
        assertThat(attachmentFileIds()).containsExactlyElementsOf(existingIds);
        assertThat(reminderIds()).containsExactlyElementsOf(originalReminderIds);
        assertThat(remindAt()).isEqualTo(REMIND_AT);
        assertStatus(existingIds.getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(existingIds.getLast(), FileUploadStatus.COMPLETED);
        assertStatus(firstNewFile.getId(), FileUploadStatus.COMPLETED);
        assertStatus(secondNewFile.getId(), FileUploadStatus.COMPLETED);
        assertStatus(memberFile.getId(), FileUploadStatus.COMPLETED);
        assertReadStateUnchanged();
    }

    private void assertReadStateUnchanged() {
        assertThat(jdbcTemplate.queryForList("SELECT read_at FROM notice_recipients WHERE notice_id = ?",
                LocalDateTime.class, notice.getId())).containsExactly(READ_AT);
    }
}
