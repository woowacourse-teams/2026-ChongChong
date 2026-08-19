package withoutc.chongchong.auth.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class AuthTokenServiceConcurrencyTest {

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("같은 사용자가 동시에 Token을 발급받아도 활성 인증 세션은 하나다")
    void keepSingleSessionOnConcurrentIssue() throws Exception {
        User user = userRepository.saveAndFlush(User.create("총총이", null));
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<IssuedTokenPair> firstFuture = executorService.submit(
                    () -> issueAfterSignal(user.getId(), ready, start)
            );
            Future<IssuedTokenPair> secondFuture = executorService.submit(
                    () -> issueAfterSignal(user.getId(), ready, start)
            );

            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();

            IssuedTokenPair first = firstFuture.get(10, SECONDS);
            IssuedTokenPair second = secondFuture.get(10, SECONDS);
            HashedRefreshToken firstHash = refreshTokenHasher.hash(first.refreshToken());
            HashedRefreshToken secondHash = refreshTokenHasher.hash(second.refreshToken());
            AuthSession currentSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();

            assertThat(first.refreshToken()).isNotEqualTo(second.refreshToken());
            assertThat(authSessionRepository.count()).isOne();
            assertThat(currentSession.getRefreshTokenHash()).isIn(firstHash, secondHash);
        } finally {
            start.countDown();
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    private IssuedTokenPair issueAfterSignal(
            Long userId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, SECONDS)) {
            throw new IllegalStateException("동시 발급 시작 신호를 기다리는 시간이 초과되었습니다.");
        }
        return authTokenService.issue(userId);
    }
}
