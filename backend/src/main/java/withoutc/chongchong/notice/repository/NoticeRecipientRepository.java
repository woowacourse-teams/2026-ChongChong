package withoutc.chongchong.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notice.entity.NoticeRecipient;

public interface NoticeRecipientRepository extends JpaRepository<NoticeRecipient, Long> {
    void deleteAllByNoticeId(Long noticeId);

    NoticeRecipient findByUserIdAndNoticeId(Long userId, Long noticeId);
}
