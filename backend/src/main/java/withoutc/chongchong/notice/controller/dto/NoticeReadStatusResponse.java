package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.NoticeRecipient;

public record NoticeReadStatusResponse(
        @Schema(description = "공지 읽음 여부", example = "true")
        boolean isRead,
        @Schema(description = "공지 읽음 처리 시각", example = "2026-08-27T10:30:00", nullable = true)
        LocalDateTime readAt
) {
    public static NoticeReadStatusResponse from(NoticeRecipient noticeRecipient) {
        return new NoticeReadStatusResponse(noticeRecipient.isRead(), noticeRecipient.getReadAt());
    }
}
