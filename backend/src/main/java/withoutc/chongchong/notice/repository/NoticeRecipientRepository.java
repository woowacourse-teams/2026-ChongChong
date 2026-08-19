package withoutc.chongchong.notice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notice.entity.NoticeRecipient;

public interface NoticeRecipientRepository extends JpaRepository<NoticeRecipient, Long> {
    void deleteAllByNoticeId(Long noticeId);
}
