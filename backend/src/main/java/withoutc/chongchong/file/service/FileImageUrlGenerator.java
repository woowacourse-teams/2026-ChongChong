package withoutc.chongchong.file.service;

import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class FileImageUrlGenerator {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.url-validity}")
    private Duration urlValidity;

    public String generatePresignedUrl(String objectKey, String contentType) {
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
}
