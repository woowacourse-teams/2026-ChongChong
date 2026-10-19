package withoutc.chongchong.file;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUploadUrlGenerator;
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
    @LocalServerPort
    private int port;

    private Long userId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        userId = userRepository.saveAndFlush(User.create("사용자", null)).getId();
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
