package withoutc.chongchong.notice.repository.projection;

import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.NoticeReadStatus;

public record NoticeReadStatusProjection(
        Long noticeId,
        LocalDateTime readAt
) {
    public NoticeReadStatus readStatus() {
        if (readAt == null) {
            return NoticeReadStatus.UNREAD;
        }
        return NoticeReadStatus.READ;
    }
}
