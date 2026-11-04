package withoutc.chongchong.notice.repository.projection;

import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.NoticeReadStatus;

public record NoticeCalendarProjection(
        Long id,
        String title,
        LocalDateTime createdAt,
        Long recipientId,
        LocalDateTime readAt
) {
    public NoticeReadStatus readStatus() {
        if (recipientId == null) {
            return NoticeReadStatus.NOT_ASSIGNED;
        }
        if (readAt == null) {
            return NoticeReadStatus.UNREAD;
        }
        return NoticeReadStatus.READ;
    }
}
