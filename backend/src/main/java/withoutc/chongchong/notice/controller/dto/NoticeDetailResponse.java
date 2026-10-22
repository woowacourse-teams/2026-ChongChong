package withoutc.chongchong.notice.controller.dto;

import java.time.LocalDateTime;
import java.util.List;
import withoutc.chongchong.notice.entity.Notice;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        List<NoticeAttachmentResponse> attachments
) {
    public static NoticeDetailResponse from(Notice notice, List<NoticeAttachmentResponse> attachments) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                attachments
        );
    }
}
