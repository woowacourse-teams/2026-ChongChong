package withoutc.chongchong.notice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.repository.projection.NoticeReadStatusProjection;

public interface NoticeRecipientRepository extends JpaRepository<NoticeRecipient, Long> {

    @Query("""
            SELECT new withoutc.chongchong.notice.repository.projection.NoticeReadStatusProjection(
                       recipient.notice.id,
                       recipient.readAt
                   )
            FROM NoticeRecipient recipient
            WHERE recipient.notice.id IN :noticeIds
              AND recipient.member.id = :memberId
            """)
    List<NoticeReadStatusProjection> findReadStatusesByNoticeIdsAndMemberId(
            @Param("noticeIds") List<Long> noticeIds,
            @Param("memberId") Long memberId
    );
}
