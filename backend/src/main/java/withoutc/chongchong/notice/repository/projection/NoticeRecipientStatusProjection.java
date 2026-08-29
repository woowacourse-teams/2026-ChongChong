package withoutc.chongchong.notice.repository.projection;

import java.time.LocalDateTime;

public record NoticeRecipientStatusProjection(
        Long memberId,
        String name,
        String profileImageUrl,
        boolean isRead,
        LocalDateTime lastRemindAt
) {
}
