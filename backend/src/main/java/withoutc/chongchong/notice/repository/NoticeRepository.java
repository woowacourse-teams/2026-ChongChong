package withoutc.chongchong.notice.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    Optional<Notice> findByIdAndStudyId(Long noticeId, Long studyId);

    default Notice getByIdAndStudyIdOrThrow(Long noticeId, Long studyId) {
        return findByIdAndStudyId(noticeId, studyId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notice n WHERE n.id = :noticeId AND n.study.id = :studyId")
    Optional<Notice> findByIdAndStudyIdForUpdate(@Param("noticeId") Long noticeId, @Param("studyId") Long studyId);

    default Notice getByIdAndStudyIdForUpdateOrThrow(Long noticeId, Long studyId) {
        return findByIdAndStudyIdForUpdate(noticeId, studyId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT notice FROM Notice notice WHERE notice.study.id = :studyId ORDER BY notice.id")
    List<Notice> findAllByStudyIdForUpdate(@Param("studyId") Long studyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT notice FROM Notice notice WHERE EXISTS (
                SELECT comment.id FROM Comment comment
                WHERE comment.resourceType = withoutc.chongchong.notification.entity.ResourceType.NOTICE
                  AND comment.resourceId = notice.id AND comment.authorMember.id = :memberId
            ) ORDER BY notice.id
            """)
    List<Notice> findAllByCommentAuthorMemberIdForUpdate(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT notice FROM Notice notice WHERE EXISTS (
                SELECT comment.id FROM Comment comment
                WHERE comment.resourceType = withoutc.chongchong.notification.entity.ResourceType.NOTICE
                  AND comment.resourceId = notice.id AND comment.authorUser.id = :userId
            ) ORDER BY notice.id
            """)
    List<Notice> findAllByCommentAuthorUserIdForUpdate(@Param("userId") Long userId);

    // 리더용
    @Query("""
            SELECT new withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection(
                n.id,
                n.title,
                COUNT(nr.id),
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

    // 스터디원용
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

    // 리더용
    @Query("""
            SELECT count(n.id)
            FROM Notice n
            WHERE n.study.id = :studyId
            AND EXISTS (
            SELECT nr.id
            FROM NoticeRecipient nr
            WHERE nr.notice.id = n.id AND nr.readAt IS NULL
            )
            """)
    long countIncompleteNoticeByStudyId(
            @Param("studyId") Long studyId
    );

    // 스터디원용
    @Query("""
            SELECT count(n.id)
            FROM Notice n
            WHERE n.study.id = :studyId
            AND EXISTS (
            SELECT nr.id
            FROM NoticeRecipient nr
            WHERE nr.notice.id = n.id AND nr.member.id = :memberId AND nr.readAt IS NULL
            )
            """)
    long countIncompleteNoticeByStudyIdAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );
}
