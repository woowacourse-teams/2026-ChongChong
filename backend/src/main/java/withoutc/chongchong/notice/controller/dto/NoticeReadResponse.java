package withoutc.chongchong.notice.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.NoticeRecipient;

public record NoticeReadResponse(
        LocalDateTime readAt
) {
    public static NoticeReadResponse from(NoticeRecipient noticeRecipient) {
        return new NoticeReadResponse(noticeRecipient.getReadAt());
    }
}
