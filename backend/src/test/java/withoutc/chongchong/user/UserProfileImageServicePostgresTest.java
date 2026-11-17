package withoutc.chongchong.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.service.UserProfileImageService;

class UserProfileImageServicePostgresTest extends PostgresContainerTest {

    @Autowired
    private UserProfileImageService imageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("사용자 잠금 트랜잭션에서 이미지 교체·초기화 후에도 분리한 완료 파일을 보존한다")
    void replacesAndResetsImageWithinCallerTransaction() {
        Fixture fixture = createFixture();

        transaction().executeWithoutResult(status -> {
            User user = userRepository.getByIdForUpdateOrThrow(fixture.userId());
            user.changeProfileImage(imageService.prepareForUpdate(user, fixture.newFileId()));
        });
        assertThat(currentFileId(fixture)).isEqualTo(fixture.newFileId());
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.oldFileId())).isFalse();
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.newFileId())).isTrue();

        transaction().executeWithoutResult(status -> {
            User user = userRepository.getByIdForUpdateOrThrow(fixture.userId());
            imageService.prepareForReset(user);
            user.resetProfileImage();
        });
        assertThat(currentFileId(fixture)).isNull();
        assertThat(jdbc.queryForObject("SELECT profile_image_url FROM users WHERE id = ?", String.class,
                fixture.userId())).isNull();
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.newFileId())).isFalse();
        assertThat(jdbc.queryForList("SELECT status FROM file_metadata ORDER BY id", String.class))
                .containsExactly("COMPLETED", "COMPLETED");
    }

    @Test
    @DisplayName("잠금 전에 읽은 완료 상태 대신 다른 트랜잭션에서 변경한 삭제 대기 상태로 검증한다")
    void refreshesStaleFileStateAfterLocking() {
        Fixture fixture = createFixture();

        assertThatThrownBy(() -> transaction().executeWithoutResult(status -> {
            User user = userRepository.getByIdForUpdateOrThrow(fixture.userId());
            FileMetadata staleFile = fileRepository.findById(fixture.newFileId()).orElseThrow();
            TransactionTemplate concurrent = transaction();
            concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            concurrent.executeWithoutResult(other -> jdbc.update(
                    "UPDATE file_metadata SET status = 'DELETE_PENDING' WHERE id = ?", fixture.newFileId()));
            assertThat(staleFile.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);

            user.changeProfileImage(imageService.prepareForUpdate(user, fixture.newFileId()));
        }))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE));
        assertThat(currentFileId(fixture)).isEqualTo(fixture.oldFileId());
        assertThat(jdbc.queryForObject("SELECT status FROM file_metadata WHERE id = ?", String.class,
                fixture.newFileId())).isEqualTo("DELETE_PENDING");
    }

    @Test
    @DisplayName("연결 준비 후 호출자의 저장이 실패하면 홈 변경만 롤백하고 선행 완료 파일은 보존한다")
    void rollsBackCallerChangeWithoutRevertingCompletedUpload() {
        Fixture fixture = createFixture();

        assertThatThrownBy(() -> transaction().executeWithoutResult(status -> {
            User user = userRepository.getByIdForUpdateOrThrow(fixture.userId());
            user.changeProfileImage(imageService.prepareForUpdate(user, fixture.newFileId()));
            userRepository.flush();
            throw new IllegalStateException("저장 실패");
        })).isInstanceOf(IllegalStateException.class).hasMessage("저장 실패");

        assertThat(currentFileId(fixture)).isEqualTo(fixture.oldFileId());
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.newFileId())).isFalse();
        assertThat(jdbc.queryForObject("SELECT status FROM file_metadata WHERE id = ?", String.class,
                fixture.newFileId())).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("호출자의 트랜잭션 없이 연결 준비만 단독 실행할 수 없다")
    void requiresCallerTransaction() {
        assertThatThrownBy(() -> imageService.prepareForUpdate(User.create("사용자", null), 1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private Fixture createFixture() {
        return transaction().execute(status -> {
            User user = userRepository.save(User.create("사용자", "https://example.com/social.png"));
            FileMetadata oldFile = saveImage(user);
            FileMetadata newFile = saveImage(user);
            user.changeProfileImage(oldFile);
            return new Fixture(user.getId(), oldFile.getId(), newFile.getId());
        });
    }

    private FileMetadata saveImage(User user) {
        FileMetadata file = FileMetadata.create(user, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return fileRepository.save(file);
    }

    private Long currentFileId(Fixture fixture) {
        return jdbc.queryForObject("SELECT profile_image_file_id FROM users WHERE id = ?", Long.class, fixture.userId());
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private record Fixture(Long userId, Long oldFileId, Long newFileId) {
    }
}
