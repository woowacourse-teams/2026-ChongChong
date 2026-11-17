package withoutc.chongchong.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Error;
import withoutc.chongchong.file.service.S3FileDeleter.DeleteResult;

class S3FileDeleterTest {

    private static final String PREFIX = "chongchong/files/test/uploads/";

    private final S3Client s3Client = mock(S3Client.class);
    private final S3FileDeleter deleter = new S3FileDeleter(s3Client);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deleter, "bucket", "test-bucket");
        ReflectionTestUtils.setField(deleter, "uploadPrefix", PREFIX);
    }

    @Test
    @DisplayName("업로드 접두사 밖의 키는 S3에 요청하지 않고 실패로 돌려준다")
    void rejectKeysOutsideUploadPrefix() {
        DeleteResult result = deleter.delete(List.of("other/secret"));

        assertThat(result.isDeleted("other/secret")).isFalse();
        assertThat(result.errorOf("other/secret")).isEqualTo(S3FileDeleter.OUTSIDE_UPLOAD_PREFIX);
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    @DisplayName("키별 삭제 결과를 성공과 실패로 나누고, 응답에 없는 키는 성공으로 보지 않는다")
    void splitPerKeyResults() {
        String deleted = PREFIX + "deleted";
        String denied = PREFIX + "denied";
        String missing = PREFIX + "missing";
        given(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).willReturn(DeleteObjectsResponse.builder()
                .deleted(DeletedObject.builder().key(deleted).build())
                .errors(S3Error.builder().key(denied).code("AccessDenied").build())
                .build());

        DeleteResult result = deleter.delete(List.of(deleted, denied, missing));

        assertThat(result.isDeleted(deleted)).isTrue();
        assertThat(result.isDeleted(denied)).isFalse();
        assertThat(result.isDeleted(missing)).isFalse();
        assertThat(result.errorOf(denied)).isEqualTo("AccessDenied");
        assertThat(result.errorOf(missing)).isEqualTo(S3FileDeleter.MISSING_RESULT);

        ArgumentCaptor<DeleteObjectsRequest> request = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(request.getValue().delete().objects()).extracting(ObjectIdentifier::key)
                .containsExactly(deleted, denied, missing);
    }

    @Test
    @DisplayName("S3 호출 자체가 실패하면 요청한 모든 키를 실패로 돌려준다")
    void failAllKeysWhenClientFails() {
        String key = PREFIX + "file";
        given(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .willThrow(SdkClientException.create("timeout"));

        DeleteResult result = deleter.delete(List.of(key));

        assertThat(result.isDeleted(key)).isFalse();
        assertThat(result.errorOf(key)).isEqualTo(S3FileDeleter.CLIENT_ERROR);
    }
}
