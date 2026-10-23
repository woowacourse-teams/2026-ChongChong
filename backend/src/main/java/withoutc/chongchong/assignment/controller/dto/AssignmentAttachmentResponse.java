package withoutc.chongchong.assignment.controller.dto;

import withoutc.chongchong.file.entity.FileMetadata;

public record AssignmentAttachmentResponse(
        Long fileId,
        String originalFileName,
        String contentType,
        long size,
        int sortOrder,
        String imageUrl,
        String downloadUrl
) {
    public static AssignmentAttachmentResponse of(FileMetadata file, int sortOrder, String imageUrl,
                                                  String downloadUrl) {
        return new AssignmentAttachmentResponse(
                file.getId(),
                file.getOriginalFileName(),
                file.getContentType(),
                file.getSize(),
                sortOrder,
                imageUrl,
                downloadUrl
        );
    }
}
