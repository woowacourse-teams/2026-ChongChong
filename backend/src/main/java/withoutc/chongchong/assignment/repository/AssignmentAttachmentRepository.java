package withoutc.chongchong.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.assignment.entity.AssignmentAttachment;

public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, Long> {

    boolean existsByFileId(Long fileId);

    @Query("SELECT COUNT(a) > 0 FROM SubmissionAttachment a WHERE a.file.id = :fileId")
    boolean existsSubmissionByFileId(@Param("fileId") Long fileId);

    @Query("SELECT COUNT(a) > 0 FROM NoticeAttachment a WHERE a.file.id = :fileId")
    boolean existsNoticeByFileId(@Param("fileId") Long fileId);
}
