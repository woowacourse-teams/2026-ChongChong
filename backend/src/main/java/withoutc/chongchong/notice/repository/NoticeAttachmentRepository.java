package withoutc.chongchong.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.notice.entity.NoticeAttachment;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    boolean existsByFileId(Long fileId);

    @Query("SELECT COUNT(a) > 0 FROM AssignmentAttachment a WHERE a.file.id = :fileId")
    boolean existsAssignmentByFileId(@Param("fileId") Long fileId);

    @Query("SELECT COUNT(a) > 0 FROM SubmissionAttachment a WHERE a.file.id = :fileId")
    boolean existsSubmissionByFileId(@Param("fileId") Long fileId);

    // 신규 첨부 검증에서는 파일 행 잠금을 획득한 뒤 호출한다.
    default boolean existsAnyByFileId(Long fileId) {
        return existsByFileId(fileId)
                || existsAssignmentByFileId(fileId)
                || existsSubmissionByFileId(fileId);
    }
}
