package withoutc.chongchong.file.controller.dto;

import java.time.Instant;

public record PresignedUrlCreateResponse(
        Long fileId,
        String uploadUrl,
        Instant expiresAt
) {
}
