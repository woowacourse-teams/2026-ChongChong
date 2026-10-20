package withoutc.chongchong.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;

class S3FileMetadataReaderTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3FileMetadataReader reader = new S3FileMetadataReader(s3Client);

    S3FileMetadataReaderTest() {
        ReflectionTestUtils.setField(reader, "bucket", "test-bucket");
    }

    @ParameterizedTest
    @ValueSource(longs = {1024L, 0L})
    @DisplayName("존재하는 파일은 0바이트를 포함해 실제 크기를 반환한다")
    void returnsActualSize(long size) {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(size).build());

        assertThat(reader.findSize("uploads/file")).hasValue(size);
    }

    @Test
    @DisplayName("설정된 버킷과 전달받은 객체 키로 S3를 조회한다")
    void requestsConfiguredBucketAndObjectKey() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(1024L).build());

        reader.findSize("uploads/assignment.pdf");

        ArgumentCaptor<HeadObjectRequest> captor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("uploads/assignment.pdf");
    }

    @Test
    @DisplayName("파일 미존재 응답은 조회 실패 예외 대신 빈 결과를 반환한다")
    void returnsEmptyWhenNotFound() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertThat(reader.findSize("uploads/file")).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("lookupFailures")
    @DisplayName("권한·서버·통신 오류는 원인을 보존한 파일 조회 실패 예외로 전달한다")
    void wrapsLookupFailure(RuntimeException cause) {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(cause);

        FileException exception = catchThrowableOfType(
                FileException.class, () -> reader.findSize("uploads/file"));

        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_LOOKUP_FAILED);
        assertThat(exception).hasCause(cause);
    }

    private static Stream<RuntimeException> lookupFailures() {
        return Stream.of(
                S3Exception.builder().statusCode(403).build(),
                S3Exception.builder().statusCode(500).build(),
                SdkClientException.create("Connection failed"));
    }
}
