package withoutc.chongchong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.S3FileMetadataReader;
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
class NoticeUploadCompletionApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private S3FileMetadataReader metadataReader;

    @LocalServerPort
    private int port;

    private User leader;
    private Long memberId;
    private Long studyId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("리더", null));
        User member = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("업로드 완료 연동 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        memberId = member.getId();
        studyId = study.getId();
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("완료 API를 통과한 여러 파일은 S3 재조회 없이 공지에 요청 순서대로 저장된다")
    void createNoticeAfterCompletingUploads() {
        FileMetadata first = savePendingFile("image/jpeg", 1024);
        FileMetadata second = savePendingFile("image/webp", 2048);
        when(metadataReader.findSize(first.getObjectKey())).thenReturn(OptionalLong.of(first.getSize()));
        when(metadataReader.findSize(second.getObjectKey())).thenReturn(OptionalLong.of(second.getSize()));
        complete(first).then().statusCode(204);
        complete(second).then().statusCode(204);
        assertStatus(first, FileUploadStatus.COMPLETED);
        assertStatus(second, FileUploadStatus.COMPLETED);
        List<Long> fileIds = List.of(second.getId(), first.getId());

        assertSavedNotice(createNotice(fileIds), fileIds);
        complete(first).then().statusCode(204);

        verify(metadataReader).findSize(first.getObjectKey());
        verify(metadataReader).findSize(second.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @Test
    @DisplayName("완료 요청 없이 공지 생성을 시도하면 거부하고 완료 API 성공 후 같은 파일로 재시도할 수 있다")
    void retryNoticeAfterCompletingPendingFile() {
        FileMetadata file = savePendingFile("image/png", 1024);
        List<Long> fileIds = List.of(file.getId());

        createNotice(fileIds).then().statusCode(400).body("code", equalTo("FILE_NOT_ATTACHABLE"));
        assertStatus(file, FileUploadStatus.PENDING);
        assertNoNoticeData();
        verifyNoInteractions(metadataReader);

        when(metadataReader.findSize(file.getObjectKey())).thenReturn(OptionalLong.of(file.getSize()));
        complete(file).then().statusCode(204);
        assertSavedNotice(createNotice(fileIds), fileIds);

        verify(metadataReader).findSize(file.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @Test
    @DisplayName("일부 파일의 완료가 실패하면 공지를 부분 저장하지 않고 나머지 완료 후 전체 파일로 재시도한다")
    void retryAfterCompletingRemainingFile() {
        FileMetadata first = savePendingFile("image/png", 1024);
        FileMetadata second = savePendingFile("image/png", 2048);
        when(metadataReader.findSize(first.getObjectKey())).thenReturn(OptionalLong.of(first.getSize()));
        when(metadataReader.findSize(second.getObjectKey()))
                .thenReturn(OptionalLong.empty(), OptionalLong.of(second.getSize()));
        complete(first).then().statusCode(204);
        complete(second).then().statusCode(409).body("code", equalTo("FILE_UPLOAD_NOT_FOUND"));
        List<Long> fileIds = List.of(first.getId(), second.getId());

        createNotice(fileIds).then().statusCode(400).body("code", equalTo("FILE_NOT_ATTACHABLE"));
        assertNoNoticeData();
        assertStatus(first, FileUploadStatus.COMPLETED);
        assertStatus(second, FileUploadStatus.PENDING);

        complete(second).then().statusCode(204);
        assertSavedNotice(createNotice(fileIds), fileIds);

        verify(metadataReader).findSize(first.getObjectKey());
        verify(metadataReader, times(2)).findSize(second.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @ParameterizedTest
    @ValueSource(longs = {1023, 1025})
    @DisplayName("실제 크기 불일치로 완료에 실패한 파일은 첨부할 수 없고 정상 크기로 완료한 뒤 재시도한다")
    void retryAfterActualSizeMatches(long actualSize) {
        FileMetadata file = savePendingFile("image/png", 1024);
        when(metadataReader.findSize(file.getObjectKey()))
                .thenReturn(OptionalLong.of(actualSize), OptionalLong.of(file.getSize()));
        List<Long> fileIds = List.of(file.getId());

        complete(file).then().statusCode(400).body("code", equalTo("FILE_SIZE_MISMATCH"));
        createNotice(fileIds).then().statusCode(400).body("code", equalTo("FILE_NOT_ATTACHABLE"));
        assertStatus(file, FileUploadStatus.PENDING);
        assertNoNoticeData();

        complete(file).then().statusCode(204);
        assertSavedNotice(createNotice(fileIds), fileIds);

        verify(metadataReader, times(2)).findSize(file.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @Test
    @DisplayName("S3 조회 장애로 완료에 실패한 파일은 첨부할 수 없고 조회 복구 후 완료하여 저장한다")
    void retryAfterStorageLookupRecovers() {
        FileMetadata file = savePendingFile("image/png", 1024);
        when(metadataReader.findSize(file.getObjectKey()))
                .thenThrow(new FileException(FileErrorCode.FILE_LOOKUP_FAILED))
                .thenReturn(OptionalLong.of(file.getSize()));
        List<Long> fileIds = List.of(file.getId());

        complete(file).then().statusCode(502).body("code", equalTo("FILE_LOOKUP_FAILED"));
        createNotice(fileIds).then().statusCode(400).body("code", equalTo("FILE_NOT_ATTACHABLE"));
        assertStatus(file, FileUploadStatus.PENDING);
        assertNoNoticeData();

        complete(file).then().statusCode(204);
        assertSavedNotice(createNotice(fileIds), fileIds);

        verify(metadataReader, times(2)).findSize(file.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @Test
    @DisplayName("공지 생성의 입력 검증이 실패해도 완료 파일은 유지되고 수정한 요청으로 다시 저장할 수 있다")
    void retryNoticeWithoutRepeatingUploadCompletion() {
        FileMetadata file = savePendingFile("image/png", 1024);
        when(metadataReader.findSize(file.getObjectKey())).thenReturn(OptionalLong.of(file.getSize()));
        complete(file).then().statusCode(204);
        List<Long> fileIds = List.of(file.getId());

        createNotice(fileIds, "2000-01-01T00:00:00").then().statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"));
        assertNoNoticeData();
        assertStatus(file, FileUploadStatus.COMPLETED);

        assertSavedNotice(createNotice(fileIds), fileIds);

        verify(metadataReader).findSize(file.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @ParameterizedTest
    @CsvSource({"application/pdf,1024,INVALID_NOTICE_IMAGE_TYPE", "image/png,0,INVALID_NOTICE_IMAGE_SIZE"})
    @DisplayName("공통 업로드 완료에 성공해도 공지 이미지 형식·빈 파일 정책은 별도로 적용한다")
    void rejectCompletedFileThatViolatesNoticePolicy(String contentType, long size, String errorCode) {
        FileMetadata file = savePendingFile(contentType, size);
        when(metadataReader.findSize(file.getObjectKey())).thenReturn(OptionalLong.of(size));
        complete(file).then().statusCode(204);

        createNotice(List.of(file.getId())).then().statusCode(400).body("code", equalTo(errorCode));

        assertNoNoticeData();
        assertStatus(file, FileUploadStatus.COMPLETED);
        verify(metadataReader).findSize(file.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    private FileMetadata savePendingFile(String contentType, long size) {
        return fileRepository.saveAndFlush(FileMetadata.create(
                leader, "attachment", "uploads/" + UUID.randomUUID(), contentType, size));
    }

    private Response complete(FileMetadata file) {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .post("/files/{fileId}/complete", file.getId());
    }

    private Response createNotice(List<Long> fileIds) {
        return createNotice(fileIds, "2099-01-01T00:00:00");
    }

    private Response createNotice(List<Long> fileIds, String remindAt) {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("title", "첨부 공지", "content", "공지 내용", "remindAts", List.of(remindAt),
                        "attachmentFileIds", fileIds))
                .post("/studies/{studyId}/notices", studyId);
    }

    private void assertSavedNotice(Response response, List<Long> fileIds) {
        response.then().statusCode(201);
        Long noticeId = response.jsonPath().getLong("noticeId");
        Response detail = testAuthRequest.givenAuthenticatedUser(memberId).port(port)
                .get("/studies/{studyId}/notices/{noticeId}", studyId, noticeId);
        detail.then().statusCode(200);
        assertThat(detail.jsonPath().getList("attachments.fileId", Long.class)).containsExactlyElementsOf(fileIds);
        assertThat(fileRepository.findAllById(fileIds)).hasSize(fileIds.size())
                .extracting(FileMetadata::getStatus).containsOnly(FileUploadStatus.COMPLETED);
        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_attachments")).isEqualTo(fileIds.size());
        assertThat(countRows("notice_recipients")).isOne();
        assertThat(countRows("notice_reminders")).isOne();
        assertThat(countRows("notifications")).isOne();
    }

    private void assertNoNoticeData() {
        assertThat(countRows("notices")).isZero();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notice_recipients")).isZero();
        assertThat(countRows("notice_reminders")).isZero();
        assertThat(countRows("notifications")).isZero();
        assertThat(countRows("notification_deliveries")).isZero();
    }

    private void assertStatus(FileMetadata file, FileUploadStatus status) {
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus()).isEqualTo(status);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
