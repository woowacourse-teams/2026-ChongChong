package withoutc.chongchong.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import withoutc.chongchong.file.entity.FileMetadata;

public interface FileMetaDataRepository extends JpaRepository<FileMetadata, Long> {
}
