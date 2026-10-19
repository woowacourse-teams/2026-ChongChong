package withoutc.chongchong.file.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class FileUploadUrlGenerator {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.url-validity}")
    private Duration urlValidity;

    public PresignedPutObjectRequest generatePresignedUrl(String objectKey, String contentType) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(urlValidity)
                .putObjectRequest(builder -> builder
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType))
                .build();

        return s3Presigner.presignPutObject(presignRequest);
    }
}
