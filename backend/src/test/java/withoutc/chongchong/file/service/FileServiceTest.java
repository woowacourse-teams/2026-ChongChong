package withoutc.chongchong.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateRequest;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateResponse;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.policy.FilePolicy;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class FileServiceTest {

    @Test
    @DisplayName("저장한 파일의 식별자와 저장 경로로 발급한 URL 및 만료 시각을 반환한다")
    void createPresignedUrl() {
        FileMetaDataRepository fileRepository = mock(FileMetaDataRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User owner = User.create("사용자", null);
        when(userRepository.getByIdOrThrow(1L)).thenReturn(owner);
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
            FileMetadata file = invocation.getArgument(0);
            ReflectionTestUtils.setField(file, "id", 123L);
            return file;
        });

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .build()) {
            FileUrlGenerator generator = new FileUrlGenerator(presigner);
            ReflectionTestUtils.setField(generator, "bucket", "test-upload-bucket");
            ReflectionTestUtils.setField(generator, "urlValidity", Duration.ofMinutes(3));
            FilePolicy filePolicy = new FilePolicy();
            ReflectionTestUtils.setField(filePolicy, "maxSizeBytes", 5242880L);
            FileService service = new FileService(fileRepository, userRepository, generator,
                    mock(S3FileMetadataReader.class), mock(FileUploadTransactionService.class),
                    filePolicy);
            ReflectionTestUtils.setField(service, "uploadPrefix", "chongchong/files/test/uploads/");
            Instant before = Instant.now();

            PresignedUrlCreateResponse response = service.createPresignedUrl(
                    1L, new PresignedUrlCreateRequest("과제.pdf", "application/pdf", 1024L));

            var captor = org.mockito.ArgumentCaptor.forClass(FileMetadata.class);
            org.mockito.Mockito.verify(fileRepository).save(captor.capture());
            FileMetadata saved = captor.getValue();
            assertThat(saved.getObjectKey()).startsWith("chongchong/files/test/uploads/");
            assertThat(saved.getOwner()).isSameAs(owner);
            assertThat(response.fileId()).isEqualTo(123L);
            URI uploadUri = URI.create(response.uploadUrl());
            assertThat(uploadUri.getPath()).isEqualTo("/" + saved.getObjectKey());
            assertThat(uploadUri.getHost()).startsWith("test-upload-bucket.");
            assertThat(uploadUri.getQuery()).contains("X-Amz-Expires=180", "X-Amz-Signature=");
            assertThat(response.expiresAt()).isBetween(before.plusSeconds(179), Instant.now().plusSeconds(181));

            var signed = generator.generateUploadUrl(saved.getObjectKey(), saved.getContentType());
            assertThat(signed.httpRequest().method().name()).isEqualTo("PUT");
            assertThat(signed.signedHeaders()).containsKey("content-type");
            assertThat(signed.signedHeaders().get("content-type")).containsExactly("application/pdf");
            assertThat(signed.signedHeaders().get("if-none-match")).containsExactly("*");
        }
    }
}
