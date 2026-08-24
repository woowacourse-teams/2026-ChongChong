package withoutc.chongchong.notice.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.projection.NoticeReadStatusProjection;
import withoutc.chongchong.notice.repository.projection.NoticeRecipientStatusProjection;

public interface NoticeRecipientRepository extends JpaRepository<NoticeRecipient, Long> {

    @Query("""
            SELECT new withoutc.chongchong.notice.repository.projection.NoticeRecipientStatusProjection(
                       member.id,
                       member.name,
                       member.profileImageUrl,
                       CASE WHEN recipient.readAt IS NULL THEN false ELSE true END,
                       MAX(notification.createdAt)
                   )
            FROM NoticeRecipient recipient
            JOIN recipient.member member
            LEFT JOIN Notification notification
              ON notification.recipient = member
             AND notification.resourceType = withoutc.chongchong.notification.entity.NotificationResourceType.NOTICE
             AND notification.resourceId = recipient.notice.id
             AND notification.type = withoutc.chongchong.notification.entity.NotificationType.REMIND
            WHERE recipient.notice.id = :noticeId
            GROUP BY member.id, member.name, member.profileImageUrl, recipient.readAt
            """)
    List<NoticeRecipientStatusProjection> findStatusesByNoticeId(@Param("noticeId") Long noticeId);

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

    Optional<NoticeRecipient> findByNoticeIdAndMemberId(Long noticeId, Long memberId);

    default NoticeRecipient getByNoticeIdAndMemberIdOrThrow(Long noticeId, Long memberId) {
        return findByNoticeIdAndMemberId(noticeId, memberId).orElseThrow(
                () -> new NoticeException(NoticeErrorCode.NOTICE_RECIPIENT_NOT_FOUND)
        );
    }
}
