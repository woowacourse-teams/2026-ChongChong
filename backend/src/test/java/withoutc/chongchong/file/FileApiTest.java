package withoutc.chongchong.file;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notification.support.TestNotificationSenderConfiguration;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({FileApiTest.SigningConfiguration.class, TestNotificationSenderConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileMetaDataRepository fileRepository;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;
    @Autowired
    private TestAuthRequest testAuthRequest;
    @MockitoSpyBean
    private FileUrlGenerator generator;
    @MockitoBean
    private S3FileMetadataReader metadataReader;
    @LocalServerPort
    private int port;

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @Test
    @DisplayName("파일 잠금을 3초 이상 기다리면 503을 반환하고 잠금 해제 후 재시도할 수 있다")
    void timesOutWaitingForLockAndAllowsRetry() throws Exception {
        Long fileId = createPendingFile(1024);
        when(metadataReader.findSize(anyString())).thenReturn(OptionalLong.of(1024));
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var holder = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                fileRepository.findAllByIdForUpdate(java.util.List.of(fileId));
                acquired.countDown();
                try {
                    if (!release.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("파일 잠금 해제 대기 시간이 초과되었습니다.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

            long started = System.nanoTime();
            Response response = complete(userId, fileId);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

            response.then().statusCode(503);
            assertThat(response.jsonPath().getString("code")).isEqualTo("FILE_UPLOAD_BUSY");
            assertThat(elapsedMillis).isBetween(2800L, 8000L);
            assertPending(fileId);

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            complete(userId, fileId).then().statusCode(204);
            assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("업로드 잠금 제한은 현재 트랜잭션에만 적용된다")
    void appliesLockTimeoutOnlyWithinTransaction() {
        Long fileId = createPendingFile(1024);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            fileRepository.findByIdForUpdate(fileId).orElseThrow();
            assertThat(jdbcTemplate.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("3s");
        });
        transaction.executeWithoutResult(status ->
                assertThat(jdbcTemplate.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("0"));
    }

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        userId = userRepository.saveAndFlush(User.create("사용자", null)).getId();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1024, 5242880})
    @DisplayName("실제 크기가 선언값과 같고 제한 이하이면 완료하고 재요청도 성공한다")
    void completeUpload(long size) {
        Long fileId = createPendingFile(size);
        when(metadataReader.findSize(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return OptionalLong.of(size);
        });

        complete(userId, fileId).then().statusCode(204);
        complete(userId, fileId).then().statusCode(204);

        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        verify(metadataReader, times(1)).findSize(anyString());
    }

    @Test
    @DisplayName("다른 사용자와 비인증 사용자는 업로드를 완료할 수 없다")
    void rejectsOtherOwnerAndAnonymous() {
        Long fileId = createPendingFile(1024);
        Long otherId = userRepository.saveAndFlush(User.create("다른 사용자", null)).getId();

        complete(otherId, fileId).then().statusCode(403);
        given().port(port).basePath(API_PREFIX).post("/files/{fileId}/complete", fileId)
                .then().statusCode(401);

        assertPending(fileId);
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("파일 메타데이터가 없으면 업로드 완료 요청을 거부한다")
    void rejectsMissingMetadata() {
        complete(userId, Long.MAX_VALUE).then().statusCode(404);
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("S3 파일이 없어 실패한 완료 요청은 업로드 후 재시도할 수 있다")
    void missingObjectCanBeRetriedAfterUpload() {
        Long fileId = createPendingFile(1024);
        when(metadataReader.findSize(anyString())).thenReturn(OptionalLong.empty(), OptionalLong.of(1024));

        complete(userId, fileId).then().statusCode(409);
        assertPending(fileId);
        complete(userId, fileId).then().statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(longs = {1023, 1025, 5242881})
    @DisplayName("실제 크기가 선언값과 다르거나 제한을 초과하면 대기 상태를 유지한다")
    void rejectsInvalidActualSize(long actualSize) {
        Long fileId = createPendingFile(1024);
        when(metadataReader.findSize(anyString())).thenReturn(OptionalLong.of(actualSize));

        complete(userId, fileId).then().statusCode(400);

        assertPending(fileId);
    }

    @Test
    @DisplayName("S3 조회가 실패하면 파일은 업로드 대기 상태로 남는다")
    void lookupFailureLeavesPending() {
        Long fileId = createPendingFile(1024);
        when(metadataReader.findSize(anyString())).thenThrow(new FileException(FileErrorCode.FILE_LOOKUP_FAILED));

        complete(userId, fileId).then().statusCode(502);

        assertPending(fileId);
    }

    @Test
    @DisplayName("삭제 대기 파일은 업로드를 완료할 수 없다")
    void rejectsDeletingFile() {
        Long fileId = createPendingFile(1024);
        FileMetadata file = fileRepository.findById(fileId).orElseThrow();
        file.completeUpload();
        file.markForDeletion();
        fileRepository.saveAndFlush(file);

        complete(userId, fileId).then().statusCode(409);

        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.DELETE_PENDING);
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("S3 조회 중 삭제된 파일 메타데이터를 복원하지 않는다")
    void deletionDuringLookupDoesNotRestoreMetadata() {
        Long fileId = createPendingFile(1024);
        when(metadataReader.findSize(anyString())).thenAnswer(invocation -> {
            fileRepository.deleteById(fileId);
            return OptionalLong.of(1024);
        });

        complete(userId, fileId).then().statusCode(404);

        assertThat(fileRepository.existsById(fileId)).isFalse();
    }

    @Test
    @DisplayName("같은 파일의 동시 완료 요청은 모두 성공한다")
    void concurrentCompletionSucceedsForBothRequests() throws Exception {
        Long fileId = createPendingFile(1024);
        CyclicBarrier barrier = new CyclicBarrier(2);
        when(metadataReader.findSize(anyString())).thenAnswer(invocation -> {
            barrier.await(10, TimeUnit.SECONDS);
            return OptionalLong.of(1024);
        });

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> complete(userId, fileId).statusCode());
            var second = executor.submit(() -> complete(userId, fileId).statusCode());
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(204);
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(204);
        }
        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("S3 조회 중 삭제 대기로 바뀐 상태를 완료 저장 전에 다시 확인한다")
    void stateChangedDuringLookupIsRechecked() {
        Long fileId = createPendingFile(1024);
        when(metadataReader.findSize(anyString())).thenAnswer(invocation -> {
            try (var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(() -> {
                    FileMetadata file = fileRepository.findById(fileId).orElseThrow();
                    file.completeUpload();
                    file.markForDeletion();
                    fileRepository.saveAndFlush(file);
                }).get(10, TimeUnit.SECONDS);
            }
            return OptionalLong.of(1024);
        });

        complete(userId, fileId).then().statusCode(409);

        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.DELETE_PENDING);
    }

    @Test
    @DisplayName("제한 크기를 초과한 파일은 업로드 URL을 발급하지 않는다")
    void rejectsOversizedUploadBeforeSigning() {
        request(userId, Map.of("originalFileName", "a.pdf", "contentType", "application/pdf", "size", 5242881))
                .then().statusCode(400);
        assertThat(fileRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증된 사용자에게 저장한 파일의 업로드 URL과 만료 시각을 반환한다")
    void issueUploadUrl() {
        Response response = request(userId, validBody());

        response.then().statusCode(201);
        Long fileId = response.jsonPath().getLong("fileId");
        FileMetadata saved = fileRepository.findById(fileId).orElseThrow();
        assertThat(saved.getObjectKey()).startsWith("chongchong/files/test/uploads/");
        UUID.fromString(saved.getObjectKey().substring("chongchong/files/test/uploads/".length()));
        assertThat(saved.getOwner().getId()).isEqualTo(userId);
        assertThat(saved.getOriginalFileName()).isEqualTo("과제.pdf");
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
        assertThat(saved.getSize()).isEqualTo(1024);
        assertThat(saved.getStatus()).isEqualTo(FileUploadStatus.PENDING);
        URI url = URI.create(response.jsonPath().getString("uploadUrl"));
        assertThat(url.getPath()).isEqualTo("/" + saved.getObjectKey());
        assertThat(url.getQuery()).contains("X-Amz-Signature=", "X-Amz-Expires=180");
        assertThat(Instant.parse(response.jsonPath().getString("expiresAt"))).isAfter(Instant.now());
    }

    @ParameterizedTest
    @CsvSource({
            "STUDY_IMAGE, study-images/",
            "NOTICE_ATTACHMENT, notice-attachments/",
            "ASSIGNMENT_ATTACHMENT, assignment-attachments/",
            "SUBMISSION_ATTACHMENT, submission-attachments/"
    })
    @DisplayName("업로드 용도별 경로를 저장하고 같은 경로의 업로드 URL을 반환한다")
    void issueUploadUrlByPurpose(String uploadPurpose, String purposePrefix) {
        Map<String, Object> body = new HashMap<>(validBody());
        body.put("uploadPurpose", uploadPurpose);

        Response response = request(userId, body);

        response.then().statusCode(201);
        FileMetadata saved = fileRepository.findById(response.jsonPath().getLong("fileId")).orElseThrow();
        String expectedPrefix = "chongchong/files/test/uploads/" + purposePrefix;
        assertThat(saved.getObjectKey()).startsWith(expectedPrefix);
        UUID.fromString(saved.getObjectKey().substring(expectedPrefix.length()));
        assertThat(URI.create(response.jsonPath().getString("uploadUrl")).getPath())
                .isEqualTo("/" + saved.getObjectKey());
    }

    @Test
    @DisplayName("업로드 용도가 null이면 기존 공통 경로를 사용한다")
    void issueUploadUrlWithNullPurpose() {
        Map<String, Object> body = new HashMap<>(validBody());
        body.put("uploadPurpose", null);

        Response response = request(userId, body);

        response.then().statusCode(201);
        FileMetadata saved = fileRepository.findById(response.jsonPath().getLong("fileId")).orElseThrow();
        UUID.fromString(saved.getObjectKey().substring("chongchong/files/test/uploads/".length()));
        assertThat(saved.getObjectKey()).startsWith("chongchong/files/test/uploads/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"UNKNOWN\"", "\"study-images/\"", "0", "{}"})
    @DisplayName("지원하지 않는 업로드 용도와 잘못된 자료형은 파일 저장 전에 거부한다")
    void rejectInvalidUploadPurpose(String uploadPurpose) {
        String body = """
                {"originalFileName":"a.png","contentType":"image/png","size":1024,"uploadPurpose":%s}
                """.formatted(uploadPurpose);

        Response response = request(userId, body);

        response.then().statusCode(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_REQUEST");
        assertThat(fileRepository.count()).isZero();
    }

    @Test
    @DisplayName("같은 이름의 파일도 각각 다른 저장 키와 식별자를 발급한다")
    void issueDistinctKeys() {
        Response first = request(userId, validBody());
        Response second = request(userId, validBody());

        first.then().statusCode(201);
        second.then().statusCode(201);
        assertThat(first.jsonPath().getLong("fileId")).isNotEqualTo(second.jsonPath().getLong("fileId"));
        assertThat(fileRepository.findAll()).extracting(FileMetadata::getObjectKey).doesNotHaveDuplicates();
        assertThat(fileRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("인증되지 않은 업로드 준비 요청은 거부하고 파일을 저장하지 않는다")
    void rejectAnonymous() {
        given().port(port).basePath(API_PREFIX).contentType(ContentType.JSON).body(validBody())
                .post("/files/presigned-url").then().statusCode(401);

        assertThat(fileRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 파일을 생성할 수 없다")
    void rejectMissingUser() {
        request(Long.MAX_VALUE, validBody()).then().statusCode(404);

        assertThat(fileRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"originalFileName\":\" \",\"contentType\":\"application/pdf\",\"size\":1}",
            "{\"originalFileName\":\"a.pdf\",\"contentType\":\" \",\"size\":1}",
            "{\"originalFileName\":\"a.pdf\",\"contentType\":\"application/pdf\",\"size\":-1}",
            "{\"originalFileName\":\"a.pdf\",\"contentType\":\"application/pdf\"}"
    })
    @DisplayName("필수 파일 정보가 없거나 잘못되면 400을 반환하고 저장하지 않는다")
    void rejectInvalidRequest(String body) {
        request(userId, body).then().statusCode(400);

        assertThat(fileRepository.count()).isZero();
    }

    @Test
    @DisplayName("URL 발급에 실패하면 먼저 저장한 파일 메타데이터도 롤백한다")
    void rollBackOnSigningFailure() {
        doThrow(new IllegalStateException("테스트용 서명 실패"))
                .when(generator).generateUploadUrl(anyString(), anyString());

        request(userId, validBody()).then().statusCode(500);

        assertThat(fileRepository.count()).isZero();
    }

    private Response request(Long ownerId, Object body) {
        return testAuthRequest.givenAuthenticatedUser(ownerId).port(port)
                .contentType(ContentType.JSON).body(body).post("/files/presigned-url");
    }

    private Long createPendingFile(long size) {
        return fileRepository.saveAndFlush(FileMetadata.create(userRepository.findById(userId).orElseThrow(),
                "파일.pdf", "uploads/test", "application/pdf", size)).getId();
    }

    private Response complete(Long ownerId, Long fileId) {
        return testAuthRequest.givenAuthenticatedUser(ownerId).port(port)
                .post("/files/{fileId}/complete", fileId);
    }

    private void assertPending(Long fileId) {
        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.PENDING);
    }

    private Map<String, Object> validBody() {
        return Map.of("originalFileName", "과제.pdf", "contentType", "application/pdf", "size", 1024);
    }

    @TestConfiguration
    static class SigningConfiguration {
        @Bean
        @Primary
        S3Presigner testPresigner() {
            return S3Presigner.builder().region(Region.AP_NORTHEAST_2)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                    .build();
        }
    }
}
