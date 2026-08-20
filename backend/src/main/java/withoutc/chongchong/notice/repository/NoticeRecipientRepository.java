package withoutc.chongchong.notice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.notice.entity.NoticeRecipient;

public interface NoticeRecipientRepository extends JpaRepository<NoticeRecipient, Long> {
    Optional<NoticeRecipient> findByNoticeIdAndMemberId(Long noticeId, Long memberId);
}
