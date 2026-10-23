package withoutc.chongchong.file.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class FileUrlGenerator {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.url-validity}")
    private Duration urlValidity;

    public PresignedPutObjectRequest generateUploadUrl(String objectKey, String contentType) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(urlValidity)
                .putObjectRequest(builder -> builder
                        .bucket(bucket)
                        .key(objectKey)
                        .ifNoneMatch("*")
                        .contentType(contentType))
                .build();

        return s3Presigner.presignPutObject(presignRequest);
    }

    public String generateImageUrl(String objectKey, String contentType) {
        if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
            return null;
        }
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(urlValidity)
                .getObjectRequest(builder -> builder
                        .bucket(bucket)
                        .key(objectKey)
                        .responseContentType(contentType)
                        .responseContentDisposition("inline"))
                .build();
        return s3Presigner.presignGetObject(request).url().toString();
    }

    public String generateDownloadUrl(String objectKey, String originalFileName) {
        String fileName = originalFileName.replaceAll("[\\p{Cntrl}\\\\/]", "_");
        String disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build().toString();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(urlValidity)
                .getObjectRequest(builder -> builder
                        .bucket(bucket)
                        .key(objectKey)
                        .responseContentType("application/octet-stream")
                        .responseContentDisposition(disposition))
                .build();
        return s3Presigner.presignGetObject(request).url().toString();
    }
}
