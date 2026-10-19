package withoutc.chongchong.file.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class FileUploadUrlGenerator {

    private final S3Presigner s3Presigner;
    private final String bucket;

    public FileUploadUrlGenerator(S3Presigner s3Presigner, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    public PresignedPutObjectRequest generatePresignedUrl(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest);
    }
}
