package withoutc.chongchong.file.repository;

import java.util.Optional;
import withoutc.chongchong.file.entity.FileMetadata;

public interface FileUploadLockRepository {
    Optional<FileMetadata> findByIdForUpdate(Long fileId);
}
