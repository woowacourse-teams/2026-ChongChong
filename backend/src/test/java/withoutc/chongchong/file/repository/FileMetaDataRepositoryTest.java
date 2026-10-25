package withoutc.chongchong.file.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileMetaDataRepositoryTest {

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("요청한 파일만 ID 오름차순으로 비관적 쓰기 잠금 조회한다")
    void findRequestedFilesInIdOrderForUpdate() {
        User owner = userRepository.save(User.create("파일 소유자", null));
        FileMetadata first = fileRepository.save(FileMetadata.create(owner, "a.png", "uploads/a", "image/png", 10));
        FileMetadata second = fileRepository.save(FileMetadata.create(owner, "b.png", "uploads/b", "image/png", 20));
        fileRepository.saveAndFlush(FileMetadata.create(owner, "c.png", "uploads/c", "image/png", 30));
        Long firstId = first.getId();
        Long secondId = second.getId();
        entityManager.clear();

        List<FileMetadata> files = fileRepository.findAllByIdForUpdate(List.of(secondId, firstId));

        assertThat(files).extracting(FileMetadata::getId).containsExactly(firstId, secondId);
        assertThat(files).allSatisfy(file ->
                assertThat(entityManager.getLockMode(file)).isEqualTo(LockModeType.PESSIMISTIC_WRITE));
    }
}
