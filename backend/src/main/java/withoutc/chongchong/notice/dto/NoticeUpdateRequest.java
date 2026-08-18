package withoutc.chongchong.notice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeUpdateRequest(
        String title,
        String content,
        List<LocalDateTime> remindAts
) {
}
