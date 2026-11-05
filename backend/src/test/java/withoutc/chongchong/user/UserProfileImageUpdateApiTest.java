package withoutc.chongchong.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3PresignerConfiguration.class)
class UserProfileImageUpdateApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TestAuthRequest auth;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private FileUrlGenerator urlGenerator;

    @MockitoBean
    private S3FileMetadataReader metadataReader;

    @LocalServerPort
    private int port;

    private User user;
    private Long originalFileId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        user = userRepository.saveAndFlush(User.create("사용자", "https://example.com/social.png"));
        FileMetadata original = saveFile(user, "image/png", 1024, FileUploadStatus.COMPLETED);
        connectDirectly(original);
        originalFileId = original.getId();
    }

    @AfterEach
    void tearDown() {
        verifyNoInteractions(metadataReader);
        databaseCleaner.clean();
    }

    @ParameterizedTest
    @CsvSource({"image/jpeg, 1024", "image/png, 1", "image/png, 5242880", "image/webp, 1024"})
    @DisplayName("완료된 본인 이미지로 교체하고 이름·기존 외부 URL·분리된 완료 파일을 보존한다")
    void updatesHomeProfileImage(String contentType, long size) {
        FileMetadata file = saveFile(user, contentType, size, FileUploadStatus.COMPLETED);

        Response response = update(user.getId(), file.getId());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getMap("$"))
                .containsOnlyKeys("name", "profileImageUrl")
                .containsEntry("name", "사용자");
        assertThat(response.jsonPath().getString("profileImageUrl"))
                .startsWith("https://")
                .contains(file.getObjectKey(), "X-Amz-Signature=");
        assertThat(currentFileId()).isEqualTo(file.getId());
        assertThat(jdbc.queryForObject("SELECT profile_image_url FROM users WHERE id = ?", String.class, user.getId()))
                .isEqualTo("https://example.com/social.png");
        assertThat(fileRepository.existsAnyUsageByFileId(originalFileId)).isFalse();
        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isTrue();
        assertThat(fileRepository.findById(originalFileId).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);

        Response profile = auth.givenAuthenticatedUser(user.getId()).port(port).get("/users/me");
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(profile.jsonPath().getString("profileImageUrl")).contains(file.getObjectKey());
    }

    @ParameterizedTest
    @ValueSource(longs = {1024, 5242881})
    @DisplayName("현재 이미지 재지정은 반복 성공하며 현재 용량 정책을 다시 적용하지 않는다")
    void retainsCurrentFile(long size) {
        FileMetadata current = saveFile(user, "image/png", size, FileUploadStatus.COMPLETED);
        connectDirectly(current);

        assertThat(update(user.getId(), current.getId()).statusCode()).isEqualTo(200);
        assertThat(update(user.getId(), current.getId()).statusCode()).isEqualTo(200);
        assertThat(currentFileId()).isEqualTo(current.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"fileId\":null}", "{\"fileId\":0}"})
    @DisplayName("필수 양수 파일 식별자가 아니면 입력 검증 오류를 반환한다")
    void rejectsInvalidFileId(String body) {
        assertRejected(request(user.getId(), body), 400, "INVALID_INPUT_VALUE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{", "{\"fileId\":\"not-a-number\"}"})
    @DisplayName("본문 누락과 잘못된 JSON 형식은 요청 형식 오류로 거절한다")
    void rejectsInvalidRequestBody(String body) {
        assertRejected(request(user.getId(), body), 400, "INVALID_REQUEST");
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 이미지를 변경할 수 없다")
    void rejectsAnonymousRequest() {
        Response response = given().port(port).contentType(ContentType.JSON)
                .body(Map.of("fileId", originalFileId)).patch("/api/users/me/profile-image");

        assertRejected(response, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("사용자 또는 파일이 없으면 404를 반환한다")
    void rejectsMissingUserOrFile() {
        assertRejected(update(Long.MAX_VALUE, originalFileId), 404, "USER_NOT_FOUND");
        assertRejected(update(user.getId(), Long.MAX_VALUE), 404, "FILE_NOT_FOUND");
    }

    @Test
    @DisplayName("다른 사용자의 완료 파일은 403으로 거절한다")
    void rejectsOtherOwnersFile() {
        User other = userRepository.saveAndFlush(User.create("다른 사용자", null));
        FileMetadata file = saveFile(other, "image/png", 1024, FileUploadStatus.COMPLETED);

        assertRejected(update(user.getId(), file.getId()), 403, "FILE_NOT_OWNED");
    }

    @ParameterizedTest
    @EnumSource(value = FileUploadStatus.class, names = {"PENDING", "DELETE_PENDING"})
    @DisplayName("미완료·삭제 대기 파일은 첨부 불가로 거절하고 상태를 유지한다")
    void rejectsIncompleteFiles(FileUploadStatus status) {
        FileMetadata file = saveFile(user, "image/png", 1024, status);

        assertRejected(update(user.getId(), file.getId()), 400, "FILE_NOT_ATTACHABLE");
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource({
            "image/gif, 1024, INVALID_USER_PROFILE_IMAGE_TYPE",
            "image/png, 0, INVALID_USER_PROFILE_IMAGE_SIZE",
            "image/png, 5242881, INVALID_USER_PROFILE_IMAGE_SIZE"
    })
    @DisplayName("새 이미지의 형식과 크기 정책 위반은 400으로 거절한다")
    void rejectsInvalidNewImage(String contentType, long size, String expectedCode) {
        FileMetadata file = saveFile(user, contentType, size, FileUploadStatus.COMPLETED);

        assertRejected(update(user.getId(), file.getId()), 400, expectedCode);
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("공지에서 사용 중인 파일은 홈 프로필에 재사용할 수 없다")
    void rejectsFileUsedByNotice() {
        FileMetadata file = saveFile(user, "image/png", 1024, FileUploadStatus.COMPLETED);
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명", false));
        Notice notice = Notice.create(study, "공지", "내용");
        notice.addAttachments(List.of(file));
        noticeRepository.saveAndFlush(notice);

        assertRejected(update(user.getId(), file.getId()), 400, "FILE_NOT_ATTACHABLE");
    }

    @Test
    @DisplayName("파일 잠금이 3초를 초과하면 503으로 롤백하고 잠금 해제 후 재시도할 수 있다")
    void returnsBusyOnFileLockTimeoutAndAllowsRetry() throws Exception {
        FileMetadata file = saveFile(user, "image/png", 1024, FileUploadStatus.COMPLETED);
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var holder = executor.submit(() -> holdFileLock(file.getId(), acquired, release));
            assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

            long started = System.nanoTime();
            assertRejected(update(user.getId(), file.getId()), 503, "FILE_UPLOAD_BUSY");
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(2800L, 8000L);
            assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus())
                    .isEqualTo(FileUploadStatus.COMPLETED);

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            assertThat(update(user.getId(), file.getId()).statusCode()).isEqualTo(200);
            assertThat(currentFileId()).isEqualTo(file.getId());
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void holdFileLock(Long fileId, CountDownLatch acquired, CountDownLatch release) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            fileRepository.findAllByIdForUpdate(List.of(fileId));
            acquired.countDown();
            awaitRelease(release);
        });
    }

    private void awaitRelease(CountDownLatch release) {
        try {
            assertThat(release.await(15, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void assertRejected(Response response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.jsonPath().getString("code")).isEqualTo(code);
        assertThat(currentFileId()).isEqualTo(originalFileId);
        verifyNoInteractions(urlGenerator);
    }

    private void connectDirectly(FileMetadata file) {
        user.changeProfileImage(file);
        userRepository.saveAndFlush(user);
    }

    private FileMetadata saveFile(User owner, String contentType, long size, FileUploadStatus status) {
        FileMetadata file = FileMetadata.create(owner, "image", "uploads/" + UUID.randomUUID(), contentType, size);
        if (status != FileUploadStatus.PENDING) {
            file.completeUpload();
        }
        if (status == FileUploadStatus.DELETE_PENDING) {
            file.markForDeletion();
        }
        return fileRepository.saveAndFlush(file);
    }

    private Long currentFileId() {
        return jdbc.queryForObject("SELECT profile_image_file_id FROM users WHERE id = ?", Long.class, user.getId());
    }

    private Response update(Long userId, Long fileId) {
        return request(userId, "{\"fileId\":" + fileId + "}");
    }

    private Response request(Long userId, String body) {
        return auth.givenAuthenticatedUser(userId).port(port).contentType(ContentType.JSON)
                .body(body).patch("/users/me/profile-image");
    }
}
