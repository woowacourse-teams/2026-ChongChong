package withoutc.chongchong.notice.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notice.entity.NoticeReminder;

public interface NoticeReminderRepository extends JpaRepository<NoticeReminder, Long> {

    @Query(value = """
            SELECT *
            FROM notice_reminders
            WHERE status = 'PENDING'
              AND remind_at <= :now
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NoticeReminder> findClaimableForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    @Query(value = """
            SELECT *
            FROM notice_reminders
            WHERE notice_id = :noticeId
            ORDER BY id
            FOR UPDATE
            """, nativeQuery = true)
    List<NoticeReminder> findAllByNoticeIdForUpdate(@Param("noticeId") Long noticeId);

    @Query(value = """
            SELECT *
            FROM notice_reminders
            WHERE notice_id IN (
                SELECT id
                FROM notices
                WHERE study_id = :studyId
            )
            ORDER BY id
            FOR UPDATE
            """, nativeQuery = true)
    List<NoticeReminder> findAllByStudyIdForUpdate(@Param("studyId") Long studyId);
}
