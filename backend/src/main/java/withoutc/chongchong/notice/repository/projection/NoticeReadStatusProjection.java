package withoutc.chongchong.notice.repository.projection;

import java.time.LocalDateTime;

public interface NoticeReadStatusProjection {

    Long getNoticeId();

    LocalDateTime getReadAt();

    default boolean isRead() {
        return getReadAt() != null;
    }
}
