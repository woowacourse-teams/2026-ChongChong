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
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import withoutc.chongchong.file.service.FileUploadUrlGenerator;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notification.support.TestNotificationSenderConfiguration;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:file-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@Import({FileApiTest.SigningConfiguration.class, TestNotificationSenderConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileApiTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileMetaDataRepository fileRepository;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;
    @Autowired
    private TestAuthRequest testAuthRequest;
    @MockitoSpyBean
    private FileUploadUrlGenerator generator;
    @MockitoBean
    private S3FileMetadataReader metadataReader;
    @LocalServerPort
    private int port;

    private Long userId;

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
                .when(generator).generatePresignedUrl(anyString(), anyString());

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
