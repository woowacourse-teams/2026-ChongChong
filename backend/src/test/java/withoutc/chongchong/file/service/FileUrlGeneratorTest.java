package withoutc.chongchong.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ContentDisposition;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class FileUrlGeneratorTest {

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "application/pdf"})
    @DisplayName("이미지 URL은 지원 이미지에만 inline 응답을 지정한다")
    void generatesImageUrlOnlyForSupportedTypes(String contentType) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .build()) {
            FileUrlGenerator generator = new FileUrlGenerator(presigner);
            ReflectionTestUtils.setField(generator, "bucket", "test-bucket");
            ReflectionTestUtils.setField(generator, "urlValidity", Duration.ofMinutes(10));

            String url = generator.generateImageUrl("test/object", contentType);

            if (contentType.equals("application/pdf")) {
                assertThat(url).isNull();
                return;
            }
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
            assertThat(decodedUrl).contains("response-content-disposition=inline",
                    "response-content-type=" + contentType, "X-Amz-Signature=");
        }
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    @DisplayName("다운로드 URL은 원본 파일명과 만료 시간을 포함한 attachment 응답을 지정한다")
    void generatesDownloadUrl(String fileName, String expectedName) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .build()) {
            FileUrlGenerator generator = new FileUrlGenerator(presigner);
            ReflectionTestUtils.setField(generator, "bucket", "test-bucket");
            ReflectionTestUtils.setField(generator, "urlValidity", Duration.ofMinutes(10));

            URI url = URI.create(generator.generateDownloadUrl("test/object", fileName));
            Map<String, String> query = Arrays.stream(url.getRawQuery().split("&"))
                    .map(part -> part.split("=", 2))
                    .collect(Collectors.toMap(part -> part[0],
                            part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
            assertThat(url.getScheme()).isEqualTo("https");
            assertThat(url.getPath()).isEqualTo("/test/object");
            assertThat(query.get("X-Amz-Expires")).isEqualTo("600");
            assertThat(query.get("X-Amz-Signature")).isNotBlank();
            assertThat(query.get("response-content-type")).isEqualTo("application/octet-stream");
            ContentDisposition disposition = ContentDisposition.parse(query.get("response-content-disposition"));
            assertThat(disposition.getType()).isEqualTo("attachment");
            assertThat(disposition.getFilename()).isEqualTo(expectedName);
        }
    }

    private static Stream<Arguments> fileNames() {
        return Stream.of(
                Arguments.of("report.pdf", "report.pdf"),
                Arguments.of("과제 자료.docx", "과제 자료.docx"),
                Arguments.of("발표+자료.pptx", "발표+자료.pptx"),
                Arguments.of("photo.png", "photo.png"),
                Arguments.of("folder/report.pdf", "folder_report.pdf"),
                Arguments.of("folder\\report.pdf", "folder_report.pdf"),
                Arguments.of("자료\r\n이름.pdf", "자료__이름.pdf"),
                Arguments.of("자료\"이름.pdf", "자료\"이름.pdf")
        );
    }

}
