package withoutc.chongchong.file.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.S3Exception;
import withoutc.chongchong.file.config.FileCleanupProperties;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.logging.FileCleanupLogger;
import withoutc.chongchong.file.metrics.FileCleanupMetrics;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileCleanupTransactionService;
import withoutc.chongchong.file.service.FileCleanupTransactionService.DeletionTarget;
import withoutc.chongchong.file.service.FileCleanupTransactionService.OrphanCutoffs;
import withoutc.chongchong.file.service.S3FileDeleter;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class FileCleanupPostgresTest extends PostgresContainerTest {

    private static final FileCleanupProperties PROPERTIES = new FileCleanupProperties(
            true, false, 100, 20, Duration.ofHours(1), Duration.ofHours(24), Duration.ofMinutes(10), 3);

    @MockitoBean
    private S3Client s3Client;

    @Autowired
    private FileCleanupTransactionService transactionService;

    @Autowired
    private S3FileDeleter s3FileDeleter;

    @Autowired
    private FileCleanupMetrics metrics;

    @Autowired
    private FileCleanupLogger logger;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private Clock clock;

    @Value("${aws.s3.upload-prefix}")
    private String uploadPrefix;

    private TransactionTemplate transaction;
    private Long ownerId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        transaction = new TransactionTemplate(transactionManager);
        ownerId = transaction.execute(status -> userRepository.save(User.create("파일 소유자", null)).getId());
        now = LocalDateTime.now(clock);
        givenS3DeletesEveryKey();
    }

    @Test
    @DisplayName("유예 시간이 지난 미완료·미사용 파일만 삭제 대기로 표시한다")
    void markOnlyOrphansPastGracePeriod() {
        Long expiredPending = insertFile(FileUploadStatus.PENDING, now.minusHours(2));
        Long freshPending = insertFile(FileUploadStatus.PENDING, now.minusMinutes(30));
        Long staleUnreferenced = insertFile(FileUploadStatus.COMPLETED, now.minusHours(25));
        Long freshUnreferenced = insertFile(FileUploadStatus.COMPLETED, now.minusHours(23));
        Long profileImage = insertFile(FileUploadStatus.COMPLETED, now.minusDays(3));
        Long studyImage = insertFile(FileUploadStatus.COMPLETED, now.minusDays(3));
        Long noticeAttachment = insertFile(FileUploadStatus.COMPLETED, now.minusDays(3));
        linkProfileImage(profileImage);
        linkStudyImage(studyImage);
        linkNoticeAttachment(noticeAttachment);

        FileCleanupRunResult result = worker(dryRunOff()).run();

        assertThat(result.markedPendingExpired()).isOne();
        assertThat(result.markedUnreferenced()).isOne();
        assertThat(fileExists(expiredPending)).isFalse();
        assertThat(fileExists(staleUnreferenced)).isFalse();
        assertThat(statusOf(freshPending)).isEqualTo(FileUploadStatus.PENDING);
        assertThat(statusOf(freshUnreferenced)).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(statusOf(profileImage)).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(statusOf(studyImage)).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(statusOf(noticeAttachment)).isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("드라이런은 후보 수만 집계하고 상태와 S3를 바꾸지 않는다")
    void dryRunDoesNotChangeAnything() {
        Long orphan = insertFile(FileUploadStatus.COMPLETED, now.minusDays(2));
        FileCleanupProperties dryRun = new FileCleanupProperties(
                true, true, 100, 20, Duration.ofHours(1), Duration.ofHours(24), Duration.ofMinutes(10), 3);

        FileCleanupRunResult result = worker(dryRun).run();

        assertThat(result.candidates()).isOne();
        assertThat(statusOf(orphan)).isEqualTo(FileUploadStatus.COMPLETED);
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    @DisplayName("다른 트랜잭션이 파일 행을 잠그고 있으면 기다리지 않고 건너뛰며, 그 사이 연결된 파일은 표시하지 않는다")
    void skipFileLockedByAttachingTransaction() throws Exception {
        Long fileId = insertFile(FileUploadStatus.COMPLETED, now.minusDays(2));
        OrphanCutoffs cutoffs = cutoffs();
        List<Long> candidates = transactionService.findCandidateIds(0, cutoffs, 100);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> attaching = executor.submit(() -> transaction.executeWithoutResult(status -> {
                fileRepository.getByIdForUpdateOrThrow(fileId);
                jdbcTemplate.update("UPDATE users SET profile_image_file_id = ? WHERE id = ?", fileId, ownerId);
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

            var whileLocked = transactionService.markBatch(candidates, cutoffs);
            release.countDown();
            attaching.get(10, TimeUnit.SECONDS);
            var afterCommit = transactionService.markBatch(candidates, cutoffs);

            assertThat(whileLocked.skipped()).isOne();
            assertThat(afterCommit.skipped()).isOne();
            assertThat(statusOf(fileId)).isEqualTo(FileUploadStatus.COMPLETED);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("S3 삭제가 실패하면 행을 남기고, 선점 유지 시간이 지난 뒤 다시 시도해 삭제한다")
    void retryAfterS3Failure() {
        Long fileId = insertFile(FileUploadStatus.COMPLETED, now.minusDays(2));
        willThrow(s3Exception("SlowDown")).given(s3Client).deleteObjects(any(DeleteObjectsRequest.class));

        FileCleanupRunResult failed = worker(dryRunOff()).run();

        assertThat(failed.deleteFailed()).isOne();
        assertThat(statusOf(fileId)).isEqualTo(FileUploadStatus.DELETE_PENDING);
        assertThat(deleteAttemptsOf(fileId)).isOne();

        givenS3DeletesEveryKey();
        assertThat(worker(dryRunOff()).run().deleted()).as("선점 유지 시간 안에는 다시 선점하지 않는다").isZero();

        expireClaim(fileId);
        assertThat(worker(dryRunOff()).run().deleted()).isOne();
        assertThat(fileExists(fileId)).isFalse();
    }

    @Test
    @DisplayName("최대 시도 횟수를 넘은 파일은 더 이상 선점하지 않는다")
    void stopRetryingAfterMaxAttempts() {
        Long fileId = insertFile(FileUploadStatus.COMPLETED, now.minusDays(2));
        willThrow(s3Exception("AccessDenied")).given(s3Client).deleteObjects(any(DeleteObjectsRequest.class));

        for (int attempt = 0; attempt < PROPERTIES.maxAttempts(); attempt++) {
            worker(dryRunOff()).run();
            expireClaim(fileId);
        }

        assertThat(deleteAttemptsOf(fileId)).isEqualTo(PROPERTIES.maxAttempts());
        assertThat(transactionService.claim(now, now.plusDays(1), PROPERTIES.maxAttempts(), 100)).isEmpty();
        assertThat(transactionService.findBacklog(PROPERTIES.maxAttempts()).exhausted()).isOne();
    }

    @Test
    @DisplayName("종료가 시작되면 지금 삭제 배치만 마치고 새 배치를 선점하지 않는다")
    void stopClaimingNewBatchWhenContextCloses() {
        List<Long> fileIds = List.of(
                insertFile(FileUploadStatus.COMPLETED, now.minusDays(2)),
                insertFile(FileUploadStatus.COMPLETED, now.minusDays(2)),
                insertFile(FileUploadStatus.COMPLETED, now.minusDays(2)));
        FileCleanupWorker worker = worker(new FileCleanupProperties(
                true, false, 1, 20, Duration.ofHours(1), Duration.ofHours(24), Duration.ofMinutes(10), 3));
        willAnswer(invocation -> {
            worker.onApplicationEvent(null);
            DeleteObjectsRequest request = invocation.getArgument(0);
            return DeleteObjectsResponse.builder()
                    .deleted(DeletedObject.builder().key(request.delete().objects().getFirst().key()).build())
                    .build();
        }).given(s3Client).deleteObjects(any(DeleteObjectsRequest.class));

        FileCleanupRunResult result = worker.run();

        assertThat(result.deleted()).isOne();
        assertThat(fileIds.stream().filter(this::fileExists))
                .hasSize(2)
                .allSatisfy(fileId -> {
                    assertThat(statusOf(fileId)).isEqualTo(FileUploadStatus.DELETE_PENDING);
                    assertThat(deleteAttemptsOf(fileId)).isZero();
                });
    }

    @Test
    @DisplayName("한 워커가 선점 중인 파일은 다른 워커가 중복 선점하지 않는다")
    void concurrentWorkersDoNotClaimSameFile() throws Exception {
        Long fileId = insertFile(FileUploadStatus.COMPLETED, now.minusDays(2));
        transactionService.markBatch(List.of(fileId), cutoffs());
        CompletableFuture<Void> firstClaimed = new CompletableFuture<>();
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<List<DeletionTarget>> first = executor.submit(() -> transaction.execute(status -> {
                List<DeletionTarget> targets = transactionService.claim(now, now.minusMinutes(10), 3, 100);
                firstClaimed.complete(null);
                await(release);
                return targets;
            }));
            firstClaimed.get(10, TimeUnit.SECONDS);

            List<DeletionTarget> secondWhileLocked = transactionService.claim(now, now.minusMinutes(10), 3, 100);
            release.countDown();
            List<DeletionTarget> firstTargets = first.get(10, TimeUnit.SECONDS);
            List<DeletionTarget> secondAfterCommit = transactionService.claim(now, now.minusMinutes(10), 3, 100);

            assertThat(firstTargets).extracting(DeletionTarget::id).containsExactly(fileId);
            assertThat(secondWhileLocked).isEmpty();
            assertThat(secondAfterCommit).isEmpty();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private FileCleanupWorker worker(FileCleanupProperties properties) {
        return new FileCleanupWorker(properties, transactionService, s3FileDeleter, metrics, logger, clock);
    }

    private FileCleanupProperties dryRunOff() {
        return PROPERTIES;
    }

    private OrphanCutoffs cutoffs() {
        return new OrphanCutoffs(now.minus(PROPERTIES.pendingGrace()), now.minus(PROPERTIES.unreferencedGrace()));
    }

    private Long insertFile(FileUploadStatus status, LocalDateTime createdAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO file_metadata
                    (owner_id, original_file_name, object_key, content_type, size, status, created_at, updated_at)
                VALUES (?, 'image.png', ?, 'image/png', 1024, ?, ?, ?)
                RETURNING id
                """, Long.class, ownerId, uploadPrefix + UUID.randomUUID(), status.name(), createdAt, createdAt);
    }

    private void linkProfileImage(Long fileId) {
        jdbcTemplate.update("UPDATE users SET profile_image_file_id = ? WHERE id = ?", fileId, ownerId);
    }

    private void linkStudyImage(Long fileId) {
        Long studyId = transaction.execute(status -> studyRepository.save(Study.create("스터디", "설명", false)).getId());
        jdbcTemplate.update("UPDATE studies SET image_file_id = ? WHERE id = ?", fileId, studyId);
    }

    private void linkNoticeAttachment(Long fileId) {
        transaction.executeWithoutResult(status -> {
            Study study = studyRepository.save(Study.create("공지 스터디", "설명", false));
            Notice notice = Notice.create(study, "공지", "내용");
            notice.addAttachments(List.of(fileRepository.getByIdForUpdateOrThrow(fileId)));
            noticeRepository.save(notice);
        });
    }

    private void expireClaim(Long fileId) {
        jdbcTemplate.update("UPDATE file_metadata SET delete_claimed_at = ? WHERE id = ?",
                now.minusHours(1), fileId);
    }

    private FileUploadStatus statusOf(Long fileId) {
        return FileUploadStatus.valueOf(jdbcTemplate.queryForObject(
                "SELECT status FROM file_metadata WHERE id = ?", String.class, fileId));
    }

    private int deleteAttemptsOf(Long fileId) {
        return jdbcTemplate.queryForObject(
                "SELECT delete_attempts FROM file_metadata WHERE id = ?", Integer.class, fileId);
    }

    private boolean fileExists(Long fileId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM file_metadata WHERE id = ?)", Boolean.class, fileId));
    }

    private void givenS3DeletesEveryKey() {
        willAnswer(invocation -> {
            DeleteObjectsRequest request = invocation.getArgument(0);
            return DeleteObjectsResponse.builder()
                    .deleted(request.delete().objects().stream()
                            .map(object -> DeletedObject.builder().key(object.key()).build())
                            .toList())
                    .build();
        }).given(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    private static S3Exception s3Exception(String code) {
        return (S3Exception) S3Exception.builder()
                .statusCode(503)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build())
                .build();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("대기 시간 초과");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
