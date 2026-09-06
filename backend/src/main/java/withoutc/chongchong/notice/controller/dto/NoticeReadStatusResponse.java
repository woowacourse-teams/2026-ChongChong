package withoutc.chongchong.notice.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.NoticeRecipient;

public record NoticeReadStatusResponse(
        boolean isRead,
        LocalDateTime readAt
) {
    public static NoticeReadStatusResponse from(NoticeRecipient noticeRecipient) {
        return new NoticeReadStatusResponse(noticeRecipient.isRead(), noticeRecipient.getReadAt());
    }
}
