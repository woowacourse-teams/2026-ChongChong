package withoutc.chongchong.file.controller.dto;

import java.time.Instant;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

public record PresignedUrlCreateResponse(
        Long fileId,
        String uploadUrl,
        Instant expiresAt
) {
    public static PresignedUrlCreateResponse from(Long fileId, PresignedPutObjectRequest presignedRequest) {
        return new PresignedUrlCreateResponse(
                fileId, presignedRequest.url().toExternalForm(), presignedRequest.expiration());
    }
}
