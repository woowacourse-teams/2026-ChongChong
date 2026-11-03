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
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3PresignerConfiguration.class)
class UserProfileImageResetApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

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

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        verifyNoInteractions(metadataReader);
        databaseCleaner.clean();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "true, https://example.com/social.png",
            "true, NULL",
            "false, https://example.com/social.png",
            "false, NULL"
    }, nullValues = "NULL")
    @DisplayName("업로드 파일·외부 URL 유무와 무관하게 기본 이미지로 초기화하고 반복해도 본문 없는 204를 반환한다")
    void resetsProfileImageIdempotently(boolean uploaded, String externalUrl) {
        Fixture fixture = createFixture(uploaded, externalUrl);

        assertResetSucceeded(reset(fixture.userId()));
        assertResetSucceeded(reset(fixture.userId()));

        assertDefaultProfile(fixture);
        assertFilePreserved(fixture);
    }

    @Test
    @DisplayName("홈 이미지 초기화로 분리한 완료 파일은 소유자가 다시 연결할 수 있다")
    void reattachesFileAfterReset() {
        Fixture fixture = createFixture(true, "https://example.com/social.png");
        assertResetSucceeded(reset(fixture.userId()));
        assertDefaultProfile(fixture);

        Response update = auth.givenAuthenticatedUser(fixture.userId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("fileId", fixture.fileId())).patch("/users/me/profile-image");

        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(update.jsonPath().getString("profileImageUrl")).contains("X-Amz-Signature=");
        assertThat(currentFileId(fixture)).isEqualTo(fixture.fileId());
        assertThat(externalUrl(fixture)).isNull();
        assertThat(fileRepository.findById(fixture.fileId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("본인의 홈 이미지를 초기화해도 다른 사용자의 프로필은 유지한다")
    void preservesOtherUsersProfile() {
        Fixture owner = createFixture(true, "https://example.com/owner.png");
        Fixture other = createFixture(true, "https://example.com/other.png");

        assertResetSucceeded(reset(owner.userId()));

        assertDefaultProfile(owner);
        assertUnchangedProfile(other);
        assertThat(fileRepository.existsAnyUsageByFileId(other.fileId())).isTrue();
    }

    @Test
    @DisplayName("인증되지 않은 초기화 요청은 401로 거절하고 프로필을 유지한다")
    void rejectsAnonymousRequest() {
        Fixture fixture = createFixture(true, "https://example.com/social.png");

        Response response = given().port(port).delete("/api/users/me/profile-image");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("code")).isEqualTo("AUTHENTICATION_REQUIRED");
        assertUnchangedProfile(fixture);
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("사용자가 없는 초기화 요청은 404를 반환한다")
    void rejectsMissingUser() {
        Response response = reset(Long.MAX_VALUE);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("code")).isEqualTo("USER_NOT_FOUND");
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("초기화 중 파일 잠금 시간 초과는 503으로 롤백하고 재시도할 수 있다")
    void rollsBackResetOnLockTimeoutAndAllowsRetry() throws Exception {
        Fixture fixture = createFixture(true, "https://example.com/social.png");
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var holder = executor.submit(() -> holdFileLock(fixture.fileId(), acquired, release));
            assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

            long started = System.nanoTime();
            Response response = reset(fixture.userId());

            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.jsonPath().getString("code")).isEqualTo("FILE_UPLOAD_BUSY");
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(2800L, 8000L);
            assertUnchangedProfile(fixture);
            verifyNoInteractions(urlGenerator);

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            assertResetSucceeded(reset(fixture.userId()));
            assertDefaultProfile(fixture);
            assertFilePreserved(fixture);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Fixture createFixture(boolean uploaded, String externalUrl) {
        User user = userRepository.saveAndFlush(User.create("사용자", externalUrl));
        if (!uploaded) {
            return new Fixture(user.getId(), null, externalUrl);
        }
        FileMetadata file = FileMetadata.create(user, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        fileRepository.saveAndFlush(file);
        user.changeProfileImage(file);
        userRepository.saveAndFlush(user);
        return new Fixture(user.getId(), file.getId(), externalUrl);
    }

    private void assertResetSucceeded(Response response) {
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body().asByteArray()).isEmpty();
    }

    private void assertDefaultProfile(Fixture fixture) {
        Response response = auth.givenAuthenticatedUser(fixture.userId()).port(port).get("/users/me");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getMap("$"))
                .containsOnlyKeys("name", "profileImageUrl")
                .containsEntry("name", "사용자")
                .containsEntry("profileImageUrl", null);
        assertThat(currentFileId(fixture)).isNull();
        assertThat(externalUrl(fixture)).isNull();
        verifyNoInteractions(urlGenerator);
    }

    private void assertFilePreserved(Fixture fixture) {
        if (fixture.fileId() == null) {
            return;
        }
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.fileId())).isFalse();
        assertThat(fileRepository.findById(fixture.fileId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
    }

    private void assertUnchangedProfile(Fixture fixture) {
        assertThat(currentFileId(fixture)).isEqualTo(fixture.fileId());
        assertThat(externalUrl(fixture)).isEqualTo(fixture.externalUrl());
    }

    private Long currentFileId(Fixture fixture) {
        return jdbc.queryForObject("SELECT profile_image_file_id FROM users WHERE id = ?", Long.class, fixture.userId());
    }

    private String externalUrl(Fixture fixture) {
        return jdbc.queryForObject("SELECT profile_image_url FROM users WHERE id = ?", String.class, fixture.userId());
    }

    private Response reset(Long userId) {
        return auth.givenAuthenticatedUser(userId).port(port).delete("/users/me/profile-image");
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

    private record Fixture(Long userId, Long fileId, String externalUrl) {
    }
}
