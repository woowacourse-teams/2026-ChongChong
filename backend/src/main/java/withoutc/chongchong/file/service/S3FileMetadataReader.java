package withoutc.chongchong.file.service;

import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;

@Component
@RequiredArgsConstructor
public class S3FileMetadataReader {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public OptionalLong findSize(String objectKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            HeadObjectResponse response = s3Client.headObject(request);
            return OptionalLong.of(response.contentLength());
        } catch (S3Exception e) {
            if (e.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return OptionalLong.empty();
            }
            throw new FileException(FileErrorCode.FILE_LOOKUP_FAILED, e);
        } catch (SdkClientException e) {
            throw new FileException(FileErrorCode.FILE_LOOKUP_FAILED, e);
        }
    }
}
