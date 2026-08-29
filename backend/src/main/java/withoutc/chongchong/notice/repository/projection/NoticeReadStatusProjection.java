package withoutc.chongchong.notice.repository.projection;

import java.time.LocalDateTime;

public record NoticeReadStatusProjection(
        Long noticeId,
        LocalDateTime readAt
) {
    public boolean isRead() {
        return readAt != null;
    }
}
