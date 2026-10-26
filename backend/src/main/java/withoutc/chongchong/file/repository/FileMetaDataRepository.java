package withoutc.chongchong.file.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.projection.FileUploadSnapshotProjection;

public interface FileMetaDataRepository extends JpaRepository<FileMetadata, Long>, FileUploadLockRepository {

    @Query("""
            SELECT new withoutc.chongchong.file.repository.projection.FileUploadSnapshotProjection(
                file.objectKey,
                file.size,
                file.status,
                file.owner.id
            )
            FROM FileMetadata file
            WHERE file.id = :id
            """)
    Optional<FileUploadSnapshotProjection> findUploadSnapshotById(@Param("id") Long id);

    default FileUploadSnapshotProjection getUploadSnapshotByIdOrThrow(Long fileId) {
        return findUploadSnapshotById(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));
    }

    default FileMetadata getByIdForUpdateOrThrow(Long fileId) {
        return findByIdForUpdate(fileId)
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT file FROM FileMetadata file WHERE file.id IN :ids ORDER BY file.id")
    List<FileMetadata> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    @Query("SELECT COUNT(a) > 0 FROM NoticeAttachment a WHERE a.file.id = :fileId")
    boolean existsNoticeAttachmentByFileId(@Param("fileId") Long fileId);

    @Query("SELECT COUNT(a) > 0 FROM AssignmentAttachment a WHERE a.file.id = :fileId")
    boolean existsAssignmentAttachmentByFileId(@Param("fileId") Long fileId);

    @Query("SELECT COUNT(a) > 0 FROM SubmissionAttachment a WHERE a.file.id = :fileId")
    boolean existsSubmissionAttachmentByFileId(@Param("fileId") Long fileId);

    @Query("SELECT COUNT(study) > 0 FROM Study study WHERE study.imageFile.id = :fileId")
    boolean existsStudyImageByFileId(@Param("fileId") Long fileId);

    // 신규 연결 전에 파일 행 잠금을 획득한 트랜잭션에서 호출한다.
    default boolean existsAnyUsageByFileId(Long fileId) {
        return existsNoticeAttachmentByFileId(fileId)
                || existsAssignmentAttachmentByFileId(fileId)
                || existsSubmissionAttachmentByFileId(fileId)
                || existsStudyImageByFileId(fileId);
    }
}
