package withoutc.chongchong.file.repository.projection;

import withoutc.chongchong.file.entity.FileUploadStatus;

public record FileUploadSnapshotProjection(
        String objectKey,
        long size,
        FileUploadStatus status,
        Long ownerId
) {
}
