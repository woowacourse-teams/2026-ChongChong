package withoutc.chongchong.notice;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.LongStream;
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
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.UploadedFileVerifier;
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
class NoticeCreationAttachmentApiTest extends PostgresContainerTest {

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
    private UploadedFileVerifier uploadedFileVerifier;

    @LocalServerPort
    private int port;

    private User leader;
    private User member;
    private User outsider;
    private Study study;
    private FileMetadata firstFile;
    private FileMetadata secondFile;
    private FileMetadata memberFile;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("리더", null));
        member = userRepository.save(User.create("스터디원", null));
        outsider = userRepository.save(User.create("외부 사용자", null));
        study = studyRepository.save(Study.create("공지 첨부 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        firstFile = saveFile(leader, "image/jpeg", 5242880);
        secondFile = saveFile(leader, "image/webp", 1024);
        memberFile = saveFile(member, "image/png", 1024);
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("검증을 통과한 첨부 생성은 201과 공지 ID를 반환하고 요청 순서대로 저장한다")
    void createNoticeWithAttachments() {
        List<Long> fileIds = List.of(secondFile.getId(), firstFile.getId());

        Response response = request(leader.getId(), validBody(fileIds));

        response.then().statusCode(201);
        assertThat(response.jsonPath().getMap("$")).containsOnlyKeys("noticeId");
        Long noticeId = response.jsonPath().getLong("noticeId");
        assertThat(jdbcTemplate.queryForList("""
                SELECT file_id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order
                """, Long.class, noticeId)).containsExactlyElementsOf(fileIds);
        assertThat(fileRepository.findAllById(fileIds)).extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.COMPLETED);
        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_recipients")).isOne();
        assertThat(countRows("notice_reminders")).isOne();
        assertThat(countRows("notifications")).isOne();
        verify(uploadedFileVerifier, times(2)).verify(any(FileMetadata.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":\"공지\",\"content\":\"내용\"}",
            "{\"title\":\"공지\",\"content\":\"내용\",\"attachmentFileIds\":null}",
            "{\"title\":\"공지\",\"content\":\"내용\",\"attachmentFileIds\":[]}"
    })
    @DisplayName("첨부 필드 생략·null·빈 배열은 모두 첨부 없이 공지를 생성한다")
    void createWithoutAttachments(String body) {
        request(leader.getId(), body).then().statusCode(201);

        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notifications")).isOne();
        assertThat(fileRepository.findAll()).extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.PENDING);
        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("최대 10장의 PNG 이미지는 장당 정확히 5MiB까지 첨부할 수 있다")
    void acceptMaximumCountAndSize() {
        List<Long> fileIds = LongStream.range(0, 10)
                .mapToObj(index -> saveFile(leader, "image/png", 5242880).getId()).toList();

        request(leader.getId(), validBody(fileIds)).then().statusCode(201);

        assertThat(countRows("notice_attachments")).isEqualTo(10);
        assertThat(fileRepository.findAllById(fileIds)).hasSize(10).extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("첨부 개수 제한을 넘으면 400 ATTACHMENT_LIMIT_EXCEEDED를 반환한다")
    void rejectTooManyAttachments() {
        List<Long> fileIds = LongStream.rangeClosed(1, 11).boxed().toList();

        assertRejected(request(leader.getId(), validBody(fileIds)), 400, "ATTACHMENT_LIMIT_EXCEEDED");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("중복 첨부 ID는 400 INVALID_ATTACHMENT_IDS를 반환한다")
    void rejectDuplicateAttachmentIds() {
        Object body = validBody(List.of(firstFile.getId(), firstFile.getId()));

        assertRejected(request(leader.getId(), body), 400, "INVALID_ATTACHMENT_IDS");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[null]", "[0]", "[-1]"})
    @DisplayName("null·0·음수 첨부 ID는 400 INVALID_INPUT_VALUE를 반환한다")
    void rejectInvalidAttachmentId(String idsJson) {
        String body = "{\"title\":\"공지\",\"content\":\"내용\",\"attachmentFileIds\":%s}".formatted(idsJson);

        assertRejected(request(leader.getId(), body), 400, "INVALID_INPUT_VALUE");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"invalid\"", "[\"invalid\"]"})
    @DisplayName("첨부 ID의 JSON 타입이 잘못되면 400 INVALID_REQUEST를 반환한다")
    void rejectInvalidAttachmentJsonType(String idsJson) {
        String body = "{\"title\":\"공지\",\"content\":\"내용\",\"attachmentFileIds\":%s}".formatted(idsJson);

        assertRejected(request(leader.getId(), body), 400, "INVALID_REQUEST");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("파일이 없으면 404 FILE_NOT_FOUND를 반환한다")
    void rejectMissingFile() {
        assertRejected(request(leader.getId(), validBody(List.of(Long.MAX_VALUE))), 404, "FILE_NOT_FOUND");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("타인 소유 파일은 403 FILE_NOT_OWNED를 반환한다")
    void rejectOtherUsersFile() {
        assertRejected(request(leader.getId(), validBody(List.of(memberFile.getId()))), 403, "FILE_NOT_OWNED");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("이미 공지에 연결한 파일을 재사용하면 400 FILE_NOT_ATTACHABLE을 반환한다")
    void rejectPreviouslyAttachedFile() {
        Object body = validBody(List.of(firstFile.getId()));
        request(leader.getId(), body).then().statusCode(201);

        request(leader.getId(), body).then().statusCode(400).body("code", equalTo("FILE_NOT_ATTACHABLE"));

        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_attachments")).isOne();
        assertThat(countRows("notifications")).isOne();
        assertThat(fileRepository.findById(firstFile.getId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
        verify(uploadedFileVerifier).verify(any(FileMetadata.class));
    }

    @Test
    @DisplayName("허용하지 않는 형식은 400 INVALID_NOTICE_IMAGE_TYPE을 반환한다")
    void rejectUnsupportedImageType() {
        FileMetadata file = saveFile(leader, "application/pdf", 1024);

        assertRejected(request(leader.getId(), validBody(List.of(file.getId()))), 400, "INVALID_NOTICE_IMAGE_TYPE");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5242881L})
    @DisplayName("빈 파일이나 용량 초과 파일은 400 INVALID_NOTICE_IMAGE_SIZE를 반환한다")
    void rejectInvalidImageSize(long size) {
        FileMetadata file = saveFile(leader, "image/png", size);

        assertRejected(request(leader.getId(), validBody(List.of(file.getId()))), 400, "INVALID_NOTICE_IMAGE_SIZE");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("리더가 아닌 스터디원은 본인 파일이어도 403 ACCESS_DENIED를 반환한다")
    void rejectMemberCreation() {
        assertRejected(request(member.getId(), validBody(List.of(memberFile.getId()))), 403, "ACCESS_DENIED");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("스터디에 속하지 않은 사용자는 403 STUDY_ACCESS_DENIED를 반환한다")
    void rejectOutsiderCreation() {
        assertRejected(request(outsider.getId(), validBody(List.of(firstFile.getId()))), 403, "STUDY_ACCESS_DENIED");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("인증 없는 첨부 생성 요청은 401 AUTHENTICATION_REQUIRED를 반환한다")
    void rejectAnonymousCreation() {
        Response response = given().port(port).basePath(API_PREFIX).contentType(ContentType.JSON)
                .body(validBody(List.of(firstFile.getId()))).post("/studies/{studyId}/notices", study.getId());

        assertRejected(response, 401, "AUTHENTICATION_REQUIRED");

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("일부 파일 검증 실패는 503을 반환하고 공지와 모든 파일 상태를 부분 반영하지 않는다")
    void rejectWhenOneFileVerificationFails() {
        doNothing().doThrow(new FileException(FileErrorCode.FILE_VERIFICATION_UNAVAILABLE))
                .when(uploadedFileVerifier).verify(any(FileMetadata.class));

        assertRejected(request(leader.getId(), validBody(List.of(firstFile.getId(), secondFile.getId()))),
                503, "FILE_VERIFICATION_UNAVAILABLE");

        verify(uploadedFileVerifier, times(2)).verify(any(FileMetadata.class));
    }

    private Response request(Long userId, Object body) {
        return testAuthRequest.givenAuthenticatedUser(userId).port(port).contentType(ContentType.JSON)
                .body(body).post("/studies/{studyId}/notices", study.getId());
    }

    private Map<String, Object> validBody(List<Long> fileIds) {
        return Map.of("title", "첨부 공지", "content", "공지 내용",
                "remindAts", List.of("2099-01-01T00:00:00"), "attachmentFileIds", fileIds);
    }

    private FileMetadata saveFile(User owner, String contentType, long size) {
        return fileRepository.saveAndFlush(FileMetadata.create(
                owner, "image.png", "uploads/" + UUID.randomUUID(), contentType, size
        ));
    }

    private void assertRejected(Response response, int status, String code) {
        response.then().statusCode(status).body("code", equalTo(code));
        assertThat(countRows("notices")).isZero();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notice_recipients")).isZero();
        assertThat(countRows("notice_reminders")).isZero();
        assertThat(countRows("notifications")).isZero();
        assertThat(fileRepository.findAll()).extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.PENDING);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
