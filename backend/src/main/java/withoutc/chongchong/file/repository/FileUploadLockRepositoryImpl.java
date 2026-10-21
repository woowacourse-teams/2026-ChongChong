package withoutc.chongchong.file.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;

@RequiredArgsConstructor
public class FileUploadLockRepositoryImpl implements FileUploadLockRepository {

    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<FileMetadata> findByIdForUpdate(Long fileId) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '3s'");
            }
        });
        return entityManager.createQuery("SELECT file FROM FileMetadata file WHERE file.id = :fileId", FileMetadata.class)
                .setParameter("fileId", fileId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .stream().findFirst();
    }
}
