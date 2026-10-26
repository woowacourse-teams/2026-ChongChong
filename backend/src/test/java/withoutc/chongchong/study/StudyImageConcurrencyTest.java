package withoutc.chongchong.study;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.study.controller.dto.StudyCreateRequest;
import withoutc.chongchong.study.controller.dto.StudyUpdateRequest;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.service.StudyService;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class StudyImageConcurrencyTest extends PostgresContainerTest {

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("이름 수정이 이미지 교체와 겹쳐도 새 이미지를 기존 이미지나 null로 되돌리지 않는다")
    void preservesReplacementWhenUpdatingNameConcurrently(boolean hasOriginalImage) throws Exception {
        Fixture fixture = createFixture(hasOriginalImage);

        runWhileReplacingImage(fixture, () -> studyService.updateStudy(
                fixture.userId(), fixture.studyId(), new StudyUpdateRequest("수정된 이름", null, null, null)));

        assertThat(jdbcTemplate.queryForObject("SELECT name FROM studies WHERE id = ?", String.class,
                fixture.studyId())).isEqualTo("수정된 이름");
        assertThat(jdbcTemplate.queryForObject("SELECT image_file_id FROM studies WHERE id = ?", Long.class,
                fixture.studyId())).isEqualTo(fixture.replacementFileId());
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.originalFileId())).isFalse();
    }

    @Test
    @DisplayName("스터디 삭제가 이미지 교체와 겹쳐도 교착 없이 스터디를 삭제한다")
    void deletesStudyAfterImageReplacement() throws Exception {
        Fixture fixture = createFixture(true);

        runWhileReplacingImage(fixture, () -> studyService.deleteStudy(fixture.userId(), fixture.studyId()));

        assertThat(studyRepository.existsById(fixture.studyId())).isFalse();
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.originalFileId())).isFalse();
        assertThat(fileRepository.existsAnyUsageByFileId(fixture.replacementFileId())).isFalse();
    }

    private void runWhileReplacingImage(Fixture fixture, Runnable concurrentAction) throws Exception {
        CountDownLatch releaseReplacement = new CountDownLatch(1);
        CompletableFuture<Void> replacementFlushed = new CompletableFuture<>();
        CompletableFuture<Integer> concurrentBackendPid = new CompletableFuture<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> replacement = executor.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        studyService.updateStudy(fixture.userId(), fixture.studyId(),
                                new StudyUpdateRequest(null, null, null, fixture.replacementFileId()));
                        studyRepository.flush();
                        replacementFlushed.complete(null);
                        await(releaseReplacement);
                    });
                } catch (RuntimeException | Error exception) {
                    replacementFlushed.completeExceptionally(exception);
                    throw exception;
                }
            });
            replacementFlushed.get(10, SECONDS);

            Future<?> concurrent = executor.submit(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        concurrentBackendPid.complete(jdbcTemplate.queryForObject("SELECT pg_backend_pid()",
                                Integer.class));
                        concurrentAction.run();
                    }));
            awaitLockWait(concurrentBackendPid.get(10, SECONDS));
            releaseReplacement.countDown();
            replacement.get(10, SECONDS);
            concurrent.get(10, SECONDS);
        } finally {
            releaseReplacement.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }
    }

    private void awaitLockWait(int backendPid) throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        while (!isWaitingForLock(backendPid) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(isWaitingForLock(backendPid))
                .as("후속 요청이 진행 중인 이미지 교체 트랜잭션의 잠금을 기다려야 한다")
                .isTrue();
    }

    private boolean isWaitingForLock(int backendPid) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_stat_activity
                    WHERE pid = ? AND wait_event_type = 'Lock'
                )
                """, Boolean.class, backendPid));
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, SECONDS)).as("이미지 교체 트랜잭션 동기화 대기").isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("이미지 교체 대기 중 인터럽트 발생", exception);
        }
    }

    private Fixture createFixture(boolean hasOriginalImage) {
        User user = userRepository.saveAndFlush(User.create("리더", null));
        FileMetadata original = saveCompletedImage(user);
        FileMetadata replacement = saveCompletedImage(user);
        Long studyId = studyService.createStudy(user.getId(), new StudyCreateRequest(
                "이미지 스터디", "설명", false, hasOriginalImage ? original.getId() : null)).studyId();
        return new Fixture(user.getId(), studyId, original.getId(), replacement.getId());
    }

    private FileMetadata saveCompletedImage(User owner) {
        FileMetadata file = FileMetadata.create(owner, "image.png", "study-images/" + UUID.randomUUID(),
                "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private record Fixture(Long userId, Long studyId, Long originalFileId, Long replacementFileId) {
    }
}
