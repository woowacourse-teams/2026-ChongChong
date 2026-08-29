package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.NoticeRecipient;

public record NoticeReadResponse(
        @Schema(description = "공지 읽음 처리 시각", example = "2026-08-27T10:30:00")
        LocalDateTime readAt
) {
    public static NoticeReadResponse from(NoticeRecipient noticeRecipient) {
        return new NoticeReadResponse(noticeRecipient.getReadAt());
    }
}
