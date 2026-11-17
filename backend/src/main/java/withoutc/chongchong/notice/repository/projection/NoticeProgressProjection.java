package withoutc.chongchong.notice.repository.projection;

import java.time.LocalDateTime;

public record NoticeProgressProjection(
        Long noticeId,
        long recipientCount,
        long readRecipientCount,
        LocalDateTime nextRemindAt
) {
}
