package withoutc.chongchong.user;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.controller.dto.UserProfileImageUpdateRequest;
import withoutc.chongchong.user.controller.dto.UserProfileNameUpdateRequest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.service.UserService;

@Import(TestS3PresignerConfiguration.class)
class UserProfileImageConcurrencyTest extends PostgresContainerTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository memberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @MockitoSpyBean
    private FileUrlGenerator urlGenerator;

    @MockitoBean
    private S3FileMetadataReader metadataReader;

    private User user;
    private FileMetadata original;
    private FileMetadata replacement;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        user = userRepository.saveAndFlush(User.create("사용자", "https://example.com/social.png"));
        original = saveImage();
        replacement = saveImage();
        user.changeProfileImage(original);
        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        verifyNoInteractions(metadataReader);
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("동시 이미지 교체는 사용자 잠금 순서로 반영하고 이전 파일을 사용 중으로 남기지 않는다")
    void serializesTwoReplacements() throws Exception {
        FileMetadata next = saveImage();

        runConcurrently(() -> update(replacement), () -> update(next));

        assertProfile(next, "https://example.com/social.png");
        assertUnusedCompleted(original);
        assertUnusedCompleted(replacement);
    }

    @Test
    @DisplayName("같은 파일의 동시 재지정은 두 요청 모두 성공하고 연결을 유지한다")
    void acceptsConcurrentRequestsForSameFile() throws Exception {
        runConcurrently(() -> update(replacement), () -> update(replacement));

        assertProfile(replacement, "https://example.com/social.png");
        assertUnusedCompleted(original);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("이미지 교체와 초기화가 겹치면 잠금을 나중에 얻은 요청이 반영되고 외부 URL은 복원되지 않는다")
    void serializesReplacementAndReset(boolean resetFirst) throws Exception {
        Runnable update = () -> update(replacement);
        Runnable reset = () -> userService.resetMyProfileImage(user.getId());

        runConcurrently(resetFirst ? reset : update, resetFirst ? update : reset);

        assertProfile(resetFirst ? replacement : null, null);
        assertUnusedCompleted(original);
        if (!resetFirst) {
            assertUnusedCompleted(replacement);
        }
    }

    @Test
    @DisplayName("이미지 교체 중 이름을 수정해도 새 이미지와 모든 스터디의 이름 변경을 보존한다")
    void preservesReplacementWhenNameChanges() throws Exception {
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명", false));
        memberRepository.saveAndFlush(StudyMember.create(study, user, "사용자", null, StudyMemberRole.MEMBER));

        runConcurrently(() -> update(replacement), () -> {
            var response = userService.updateMyProfileName(user.getId(), new UserProfileNameUpdateRequest("새이름"));
            assertThat(response.name()).isEqualTo("새이름");
            assertThat(response.profileImageUrl()).contains(replacement.getObjectKey());
        });

        assertProfile(replacement, "https://example.com/social.png");
        assertThat(userRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo("새이름");
        assertThat(jdbc.queryForList("SELECT name FROM study_members WHERE user_id = ?", String.class, user.getId()))
                .containsExactly("새이름");
    }

    @Test
    @DisplayName("동시 초기화는 모두 성공하고 이미지 연결과 외부 URL을 비운다")
    void acceptsConcurrentResets() throws Exception {
        Runnable reset = () -> userService.resetMyProfileImage(user.getId());

        runConcurrently(reset, reset);

        assertProfile(null, null);
        assertUnusedCompleted(original);
        assertUnusedCompleted(replacement);
    }

    @Test
    @DisplayName("조회 URL 생성 실패 시 이미지 교체를 롤백하고 완료 파일을 보존하여 재시도할 수 있다")
    void rollsBackReplacementWhenUrlGenerationFails() {
        doThrow(new IllegalStateException("URL 생성 실패")).doCallRealMethod()
                .when(urlGenerator).generateImageUrl(replacement.getObjectKey(), replacement.getContentType());

        assertThatThrownBy(() -> update(replacement))
                .isInstanceOf(IllegalStateException.class).hasMessage("URL 생성 실패");

        assertProfile(original, "https://example.com/social.png");
        assertUnusedCompleted(replacement);

        update(replacement);

        assertProfile(replacement, "https://example.com/social.png");
        assertUnusedCompleted(original);
    }

    private void runConcurrently(Runnable firstAction, Runnable secondAction) throws Exception {
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CompletableFuture<Void> firstFlushed = new CompletableFuture<>();
        CompletableFuture<Integer> secondPid = new CompletableFuture<>();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> runFirst(firstAction, firstFlushed, releaseFirst));
            firstFlushed.get(10, SECONDS);
            var second = executor.submit(() -> runSecond(secondAction, secondPid));
            awaitLockWait(secondPid.get(10, SECONDS));
            releaseFirst.countDown();
            first.get(10, SECONDS);
            second.get(10, SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }
    }

    private void runFirst(Runnable action, CompletableFuture<Void> flushed, CountDownLatch release) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    flushAndWait(action, flushed, release));
        } catch (RuntimeException | Error exception) {
            flushed.completeExceptionally(exception);
            throw exception;
        }
    }

    private void flushAndWait(Runnable action, CompletableFuture<Void> flushed, CountDownLatch release) {
        action.run();
        userRepository.flush();
        flushed.complete(null);
        await(release);
    }

    private void runSecond(Runnable action, CompletableFuture<Integer> backendPid) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            backendPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
            action.run();
        });
    }

    private void awaitLockWait(int backendPid) throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        while (!isWaitingForLock(backendPid) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(isWaitingForLock(backendPid)).as("두 번째 요청이 사용자 잠금을 기다리는지 확인").isTrue();
    }

    private boolean isWaitingForLock(int backendPid) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock'
                )
                """, Boolean.class, backendPid));
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void update(FileMetadata file) {
        userService.updateMyProfileImage(user.getId(), new UserProfileImageUpdateRequest(file.getId()));
    }

    private FileMetadata saveImage() {
        FileMetadata file = FileMetadata.create(user, "image.png", "profiles/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private void assertProfile(FileMetadata expected, String externalUrl) {
        Long fileId = jdbc.queryForObject("SELECT profile_image_file_id FROM users WHERE id = ?", Long.class,
                user.getId());
        assertThat(fileId).isEqualTo(expected == null ? null : expected.getId());
        assertThat(jdbc.queryForObject("SELECT profile_image_url FROM users WHERE id = ?", String.class, user.getId()))
                .isEqualTo(externalUrl);
        if (expected != null) {
            assertThat(fileRepository.existsAnyUsageByFileId(expected.getId())).isTrue();
            assertThat(fileRepository.findById(expected.getId()).orElseThrow().getStatus())
                    .isEqualTo(FileUploadStatus.COMPLETED);
        }
    }

    private void assertUnusedCompleted(FileMetadata file) {
        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isFalse();
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }
}
