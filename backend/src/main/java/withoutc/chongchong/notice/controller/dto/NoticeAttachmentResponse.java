package withoutc.chongchong.notice.controller.dto;

import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.notice.entity.NoticeAttachment;

public record NoticeAttachmentResponse(
        Long fileId,
        String originalFileName,
        String contentType,
        long size,
        int sortOrder,
        String imageUrl
) {
    public static NoticeAttachmentResponse from(NoticeAttachment attachment, String imageUrl) {
        FileMetadata file = attachment.getFile();
        return new NoticeAttachmentResponse(
                file.getId(),
                file.getOriginalFileName(),
                file.getContentType(),
                file.getSize(),
                attachment.getSortOrder(),
                imageUrl
        );
    }
}
