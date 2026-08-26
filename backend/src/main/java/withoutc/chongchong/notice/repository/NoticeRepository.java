package withoutc.chongchong.notice.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Query("""
            SELECT n
            FROM Notice n
            WHERE n.study.id = :studyId
              AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
            """)
    List<Notice> findByCursor(
            @Param("studyId") Long studyId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    default Notice getByIdOrThrow(Long noticeId) {
        return findById(noticeId).orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    List<Notice> findAllByStudyId(Long studyId);

    @Query("""
            SELECT new withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection(
                n.id,
                n.title,
                COUNT(nr.readAt)
            )
            FROM Notice n
            JOIN NoticeRecipient nr
              ON nr.notice = n
            WHERE n.study.id = :studyId
            GROUP BY n.id, n.title, n.createdAt
            HAVING COUNT(nr.readAt) < COUNT(nr.id)
            ORDER BY n.createdAt DESC
            """)
    List<LeaderNoticeSummaryProjection> findIncompleteNoticeSummariesByStudyId(
            @Param("studyId") Long studyId
    );

    @Query("""
            SELECT n
            FROM Notice n
            WHERE n.study.id = :studyId
            AND EXISTS (
            SELECT nr.id
            FROM NoticeRecipient nr
            WHERE nr.notice = n AND nr.member.id = :memberId
            AND nr.readAt IS NULL
            )
            ORDER BY n.createdAt DESC
            """)
    List<Notice> findIncompleteNoticesByStudyIdAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );

    void deleteAllByStudyId(Long studyId);
}
