package withoutc.chongchong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
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

@Import(TestS3PresignerConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "aws.s3.url-validity=7m")
class NoticeDetailAttachmentApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeRecipientRepository recipientRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private FileUrlGenerator imageUrlGenerator;

    @MockitoBean
    private S3FileMetadataReader metadataReader;

    @LocalServerPort
    private int port;

    private Long studyId;
    private Long noticeId;
    private Long memberId;
    private Long fileOwnerId;
    private Long firstFileId;
    private Long secondFileId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        User leader = userRepository.save(User.create("현재 리더", null));
        User formerMember = userRepository.save(User.create("기존 첨부 업로더", null));
        Study study = studyRepository.save(Study.create("공지 상세 스터디", "설명", false));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        FileMetadata firstFile = saveCompletedFile(formerMember, "first.png", "image/png", 1024);
        FileMetadata secondFile = saveCompletedFile(formerMember, "second.webp", "image/webp", 2048);
        Notice notice = Notice.create(study, "이미지 첨부 공지", "공지 내용");
        notice.addAttachments(List.of(secondFile, firstFile));
        noticeRepository.saveAndFlush(notice);

        User member = userRepository.save(User.create("공지 작성 후 가입한 스터디원", null));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        studyId = study.getId();
        noticeId = notice.getId();
        memberId = member.getId();
        fileOwnerId = formerMember.getId();
        firstFileId = firstFile.getId();
        secondFileId = secondFile.getId();
    }

    @AfterEach
    void cleanDatabase() {
        verifyNoInteractions(metadataReader);
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("기존 첨부 메타데이터와 이미지 URL을 표시 순서로 반환한다")
    void getAttachmentsInDisplayOrder() {
        Response response = request(memberId);

        response.then().statusCode(200)
                .body("id", equalTo(noticeId.intValue()))
                .body("attachments.fileId", contains(secondFileId.intValue(), firstFileId.intValue()))
                .body("attachments.originalFileName", contains("second.webp", "first.png"))
                .body("attachments.contentType", contains("image/webp", "image/png"))
                .body("attachments.size", contains(2048, 1024))
                .body("attachments.sortOrder", contains(0, 1));
        List<Map<String, Object>> attachments = response.jsonPath().getList("attachments");
        assertThat(attachments).allSatisfy(attachment -> {
            assertThat(attachment)
                    .containsOnlyKeys("fileId", "originalFileName", "contentType", "size", "sortOrder", "imageUrl");
            assertThat((String) attachment.get("imageUrl")).startsWith("https://").contains("X-Amz-Signature=");
        });
        assertThat(fileRepository.findById(firstFileId).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(fileRepository.findById(secondFileId).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("공지 작성 후 가입한 스터디원도 첨부를 조회하며 수신자로 추가되지 않는다")
    void readOlderNoticeWithoutAddingRecipient() {
        assertThat(countRecipients()).isZero();

        request(memberId).then().statusCode(200)
                .body("attachments.fileId", contains(secondFileId.intValue(), firstFileId.intValue()));

        assertThat(countRecipients()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("지원 이미지 URL에 파일 경로와 응답 형식 및 설정한 유효기간을 반영한다")
    void returnsSignedImageUrlWithConfiguredValidity(String contentType) {
        User owner = userRepository.findById(fileOwnerId).orElseThrow();
        FileMetadata image = saveCompletedFile(owner, "첨부 이미지 + 테스트", contentType, 1024);
        Notice notice = saveNotice(List.of(image));

        Response response = request(memberId, studyId, notice.getId());

        response.then().statusCode(200);
        URI imageUrl = URI.create(response.jsonPath().getString("attachments[0].imageUrl"));
        assertThat(imageUrl.getScheme()).isEqualTo("https");
        assertThat(imageUrl.getHost()).isEqualTo("test-upload-bucket.s3.ap-northeast-2.amazonaws.com");
        assertThat(imageUrl.getPath()).isEqualTo("/" + image.getObjectKey());
        Map<String, String> query = queryParameters(imageUrl);
        assertThat(query)
                .containsEntry("X-Amz-Algorithm", "AWS4-HMAC-SHA256")
                .containsEntry("X-Amz-Expires", "420")
                .containsEntry("response-content-type", contentType)
                .containsEntry("response-content-disposition", "inline");
        assertThat(query.get("X-Amz-Signature")).matches("[0-9a-f]{64}");
        assertThat(query.get("X-Amz-Credential")).startsWith("test-access-key/")
                .endsWith("/ap-northeast-2/s3/aws4_request");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "image/gif"})
    @DisplayName("공통 생성기가 지원하지 않는 기존 첨부도 메타데이터와 imageUrl null을 반환한다")
    void returnsNullImageUrlForUnsupportedStoredFile(String contentType) {
        User owner = userRepository.findById(fileOwnerId).orElseThrow();
        FileMetadata file = saveCompletedFile(owner, "legacy-file", contentType, 2048);
        Notice notice = saveNotice(List.of(file));

        Response response = request(memberId, studyId, notice.getId());

        response.then().statusCode(200)
                .body("attachments.fileId", contains(file.getId().intValue()))
                .body("attachments.contentType", contains(contentType))
                .body("attachments.size", contains(2048))
                .body("attachments.sortOrder", contains(0));
        Map<String, Object> attachment = response.jsonPath().getMap("attachments[0]");
        assertThat(attachment).containsKey("imageUrl").containsEntry("imageUrl", null);
    }

    @Test
    @DisplayName("무첨부 공지는 빈 배열을 반환하며 이미지 URL을 생성하지 않는다")
    void returnsEmptyAttachmentsWithoutGeneratingUrl() {
        Notice notice = saveNotice(List.of());

        Response response = request(memberId, studyId, notice.getId());

        response.then().statusCode(200);
        assertThat(response.jsonPath().getList("attachments")).isEmpty();
        verifyNoInteractions(imageUrlGenerator);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("상세 재조회마다 URL을 발급하면서 파일·첨부 관계·공지·수신 상태를 보존한다")
    void refreshesUrlsWithoutChangingPersistedState(boolean read) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, memberId);
        Notice notice = noticeRepository.getByIdAndStudyIdOrThrow(noticeId, studyId);
        NoticeRecipient recipient = NoticeRecipient.create(member, notice);
        if (read) {
            recipient.markAsRead(LocalDateTime.of(2026, 10, 21, 10, 0));
        }
        recipientRepository.saveAndFlush(recipient);
        Map<String, List<Map<String, Object>>> before = persistedState();

        request(memberId).then().statusCode(200)
                .body("attachments.fileId", contains(secondFileId.intValue(), firstFileId.intValue()));
        request(memberId).then().statusCode(200)
                .body("attachments.fileId", contains(secondFileId.intValue(), firstFileId.intValue()));

        assertThat(persistedState()).isEqualTo(before);
        verify(imageUrlGenerator, times(2)).generateImageUrl("uploads/first.png", "image/png");
        verify(imageUrlGenerator, times(2)).generateImageUrl("uploads/second.webp", "image/webp");
    }

    @Test
    @DisplayName("첨부 파일 소유자여도 현재 스터디원이 아니면 공지 첨부를 조회할 수 없다")
    void rejectFileOwnerOutsideStudy() {
        assertRejectedWithoutGeneratingUrl(request(fileOwnerId), 403, "STUDY_ACCESS_DENIED");
    }

    @Test
    @DisplayName("다른 스터디의 공지 경로에서는 이미지 URL을 생성하지 않는다")
    void rejectNoticeFromAnotherStudy() {
        Study otherStudy = studyRepository.save(Study.create("다른 스터디", "설명", false));
        User member = userRepository.findById(memberId).orElseThrow();
        studyMemberRepository.save(StudyMember.create(otherStudy, member, "스터디원", null, StudyMemberRole.MEMBER));

        Response response = request(memberId, otherStudy.getId(), noticeId);

        assertRejectedWithoutGeneratingUrl(response, 404, "NOTICE_NOT_FOUND");
    }

    @Test
    @DisplayName("없는 공지는 이미지 URL을 생성하지 않는다")
    void rejectMissingNotice() {
        Response response = request(memberId, studyId, Long.MAX_VALUE);

        assertRejectedWithoutGeneratingUrl(response, 404, "NOTICE_NOT_FOUND");
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 공지 첨부를 조회할 수 없다")
    void rejectAnonymousUser() {
        Response response = RestAssured.given().port(port).basePath(API_PREFIX)
                .get("/studies/{studyId}/notices/{noticeId}", studyId, noticeId);

        assertRejectedWithoutGeneratingUrl(response, 401, "AUTHENTICATION_REQUIRED");
    }

    private Response request(Long userId) {
        return request(userId, studyId, noticeId);
    }

    private Response request(Long userId, Long targetStudyId, Long targetNoticeId) {
        return testAuthRequest.givenAuthenticatedUser(userId).port(port)
                .get("/studies/{studyId}/notices/{noticeId}", targetStudyId, targetNoticeId);
    }

    private void assertRejectedWithoutGeneratingUrl(Response response, int status, String code) {
        response.then().statusCode(status).body("code", equalTo(code));
        assertThat(response.asString()).doesNotContain("imageUrl", "X-Amz-Signature");
        verifyNoInteractions(imageUrlGenerator);
    }

    private Notice saveNotice(List<FileMetadata> files) {
        Study study = studyRepository.findById(studyId).orElseThrow();
        Notice notice = Notice.create(study, "첨부 조회 검증 공지", "내용");
        notice.addAttachments(files);
        return noticeRepository.saveAndFlush(notice);
    }

    private Map<String, String> queryParameters(URI url) {
        return Arrays.stream(url.getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(
                        parameter -> URLDecoder.decode(parameter[0], StandardCharsets.UTF_8),
                        parameter -> URLDecoder.decode(parameter[1], StandardCharsets.UTF_8)
                ));
    }

    private Map<String, List<Map<String, Object>>> persistedState() {
        return Map.of(
                "notice", jdbcTemplate.queryForList("SELECT * FROM notices WHERE id = ?", noticeId),
                "attachments", jdbcTemplate.queryForList(
                        "SELECT * FROM notice_attachments WHERE notice_id = ? ORDER BY id", noticeId),
                "files", jdbcTemplate.queryForList("SELECT * FROM file_metadata ORDER BY id"),
                "recipients", jdbcTemplate.queryForList(
                        "SELECT * FROM notice_recipients WHERE notice_id = ? ORDER BY id", noticeId)
        );
    }

    private FileMetadata saveCompletedFile(User owner, String name, String contentType, long size) {
        FileMetadata file = FileMetadata.create(owner, name, "uploads/" + name, contentType, size);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private Long countRecipients() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notice_recipients WHERE notice_id = ?", Long.class, noticeId
        );
    }
}
