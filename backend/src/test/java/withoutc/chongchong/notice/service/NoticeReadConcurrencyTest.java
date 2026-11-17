package withoutc.chongchong.notice.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.notice.controller.dto.NoticeReadResponse;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class NoticeReadConcurrencyTest extends PostgresContainerTest {

    private static final long TIMEOUT = 15;

    @Autowired
    private NoticeReadService noticeReadService;

    @Autowired
    private UserRepository users;

    @Autowired
    private StudyRepository studies;

    @Autowired
    private StudyMemberRepository members;

    @Autowired
    private NoticeRepository notices;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        cleaner.clean();
    }

    @Test
    @DisplayName("같은 수신자의 읽음 요청이 겹쳐도 최초 읽은 시각을 보존하고 두 요청 모두 그 시각을 응답한다")
    void preserveFirstReadAtOnConcurrentRead() throws Exception {
        Fixture f = fixture();
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> firstRead = new CompletableFuture<>();
        CompletableFuture<Integer> secondPid = new CompletableFuture<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // 첫 번째 요청은 읽음 처리를 마친 뒤 Commit하지 않고 기다린다.
            Future<NoticeReadResponse> first = executor.submit(() -> inTransaction(() -> {
                NoticeReadResponse response = markAsRead(f);
                firstRead.complete(null);
                await(release);
                return response;
            }));
            firstRead.get(TIMEOUT, SECONDS);

            // 두 번째 요청은 첫 번째 요청이 Commit하기 전에 시작한다.
            Future<NoticeReadResponse> second = executor.submit(() -> inTransaction(() -> {
                secondPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                return markAsRead(f);
            }));
            awaitCompletionOrLockWait(second, secondPid.get(TIMEOUT, SECONDS));
            release.countDown();

            LocalDateTime firstReadAt = first.get(TIMEOUT, SECONDS).readAt();
            LocalDateTime secondReadAt = second.get(TIMEOUT, SECONDS).readAt();
            LocalDateTime storedReadAt = jdbc.queryForObject(
                    "SELECT read_at FROM notice_recipients WHERE notice_id = ?", LocalDateTime.class, f.noticeId());

            assertThat(firstReadAt).isNotNull();
            assertThat(secondReadAt).isEqualTo(firstReadAt);
            assertThat(storedReadAt).isEqualTo(firstReadAt);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT, SECONDS)).isTrue();
        }
    }

    private NoticeReadResponse markAsRead(Fixture f) {
        return noticeReadService.markAsRead(f.userId(), f.studyId(), f.noticeId());
    }

    private <T> T inTransaction(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(TIMEOUT, SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    /**
     * 두 번째 요청이 끝났거나 DB 잠금을 기다리기 시작할 때까지 기다린다. 잠금이 없으면 두 번째 요청이 먼저 Commit되고,
     * 잠금이 있으면 첫 번째 요청의 Commit을 기다린다.
     */
    private void awaitCompletionOrLockWait(Future<?> second, int pid) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (second.isDone() || Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                        WHERE pid = ? AND wait_event_type = 'Lock')
                    """, Boolean.class, pid))) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("두 번째 읽음 요청이 끝나거나 DB 잠금을 기다리지 않았다");
    }

    private Fixture fixture() {
        User user = users.save(User.create("수신자", null));
        Study study = studies.save(Study.create("읽음 경합 스터디", "설명", false));
        StudyMember member = members.saveAndFlush(
                StudyMember.create(study, user, "수신자", null, StudyMemberRole.LEADER));
        Notice notice = Notice.create(study, "공지", "내용");
        notice.addRecipients(List.of(member));
        notices.saveAndFlush(notice);
        return new Fixture(user.getId(), study.getId(), notice.getId());
    }

    private record Fixture(Long userId, Long studyId, Long noticeId) {
    }
}
