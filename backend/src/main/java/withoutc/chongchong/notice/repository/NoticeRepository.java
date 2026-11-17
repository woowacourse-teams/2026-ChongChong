package withoutc.chongchong.notice.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
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
import withoutc.chongchong.notice.repository.projection.NoticeCalendarProjection;
import withoutc.chongchong.notice.repository.projection.NoticeProgressProjection;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 리더 공지 목록: 컬렉션을 올리지 않고 수신자 수, 읽은 수신자 수, 다음 리마인드 시각을 한 번에 집계한다.
    @Query("""
            SELECT new withoutc.chongchong.notice.repository.projection.NoticeProgressProjection(
                n.id,
                (SELECT COUNT(r.id) FROM NoticeRecipient r WHERE r.notice = n),
                (SELECT COUNT(r.id) FROM NoticeRecipient r WHERE r.notice = n AND r.readAt IS NOT NULL),
                (SELECT MIN(rm.remindAt) FROM NoticeReminder rm
                 WHERE rm.notice = n
                   AND rm.status = withoutc.chongchong.notice.entity.NoticeReminderStatus.PENDING)
            )
            FROM Notice n
            WHERE n.id IN :noticeIds
            """)
    List<NoticeProgressProjection> findProgressByIdIn(@Param("noticeIds") List<Long> noticeIds);

    @Query("""
            SELECT new withoutc.chongchong.notice.repository.projection.NoticeCalendarProjection(
                notice.id, notice.title, notice.createdAt, recipient.id, recipient.readAt
            )
            FROM Notice notice
            LEFT JOIN NoticeRecipient recipient
              ON recipient.notice = notice AND recipient.member.id = :memberId
            WHERE notice.study.id = :studyId
              AND notice.createdAt >= :startAt AND notice.createdAt < :endAt
            """)
    List<NoticeCalendarProjection> findCalendarNotices(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

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
              AND n.createdAt > :createdAfter
            GROUP BY n.id, n.title, n.createdAt
            HAVING COUNT(nr.readAt) < COUNT(nr.id)
            ORDER BY n.createdAt DESC
            """)
    List<LeaderNoticeSummaryProjection> findIncompleteNoticeSummariesByStudyId(
            @Param("studyId") Long studyId,
            @Param("createdAfter") LocalDateTime createdAfter
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
              AND n.createdAt > :createdAfter
            AND EXISTS (
            SELECT nr.id
            FROM NoticeRecipient nr
            WHERE nr.notice.id = n.id AND nr.readAt IS NULL
            )
            """)
    long countIncompleteNoticeByStudyId(
            @Param("studyId") Long studyId,
            @Param("createdAfter") LocalDateTime createdAfter
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
