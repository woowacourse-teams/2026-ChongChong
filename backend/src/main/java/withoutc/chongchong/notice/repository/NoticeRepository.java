package withoutc.chongchong.notice.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;

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

    void deleteAllByStudyId(Long studyId);
}
