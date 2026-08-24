package withoutc.chongchong.notification.repository.projection;

import java.time.LocalDateTime;

public record MemberLastRemindAtProjection(
        Long memberId,
        LocalDateTime lastRemindAt
) {
}
