package withoutc.chongchong.file.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import withoutc.chongchong.file.entity.FileMetadata;

public interface FileMetaDataRepository extends JpaRepository<FileMetadata, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT file FROM FileMetadata file WHERE file.id IN :ids ORDER BY file.id")
    List<FileMetadata> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}
