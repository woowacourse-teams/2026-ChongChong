package withoutc.chongchong.auth.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class AuthTokenServiceRefreshConcurrencyTest {

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
    @DisplayName("같은 Refresh Token의 동시 회전 요청은 하나만 성공한다")
    void allowOnlyOneConcurrentRotation() throws Exception {
        User user = userRepository.saveAndFlush(User.create("총총이", null));
        IssuedTokenPair original = authTokenService.issue(user.getId());
        HashedRefreshToken originalHash = refreshTokenHasher.hash(original.refreshToken());
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<RotationResult> firstFuture = executorService.submit(
                    () -> rotateAfterSignal(original.refreshToken(), ready, start)
            );
            Future<RotationResult> secondFuture = executorService.submit(
                    () -> rotateAfterSignal(original.refreshToken(), ready, start)
            );

            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();

            List<RotationResult> results = List.of(
                    firstFuture.get(10, SECONDS),
                    secondFuture.get(10, SECONDS)
            );
            List<RotationResult> successes = results.stream()
                    .filter(RotationResult::isSuccess)
                    .toList();
            List<AuthErrorCode> failures = results.stream()
                    .filter(result -> !result.isSuccess())
                    .map(RotationResult::errorCode)
                    .toList();

            assertThat(successes).hasSize(1);
            assertThat(failures).containsExactly(AuthErrorCode.INVALID_REFRESH_TOKEN);

            AuthSession currentSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
            HashedRefreshToken successfulHash = refreshTokenHasher.hash(
                    successes.getFirst().tokenPair().refreshToken()
            );

            assertThat(authSessionRepository.count()).isOne();
            assertThat(currentSession.getRefreshTokenHash())
                    .isEqualTo(successfulHash)
                    .isNotEqualTo(originalHash);
        } finally {
            start.countDown();
            executorService.shutdownNow();
            executorService.awaitTermination(5, SECONDS);
        }
    }

    private RotationResult rotateAfterSignal(
            RawRefreshToken refreshToken,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, SECONDS)) {
            throw new IllegalStateException("동시 회전 시작 신호를 기다리는 시간이 초과되었습니다.");
        }

        try {
            return RotationResult.success(authTokenService.rotate(refreshToken));
        } catch (AuthException exception) {
            return RotationResult.failure((AuthErrorCode) exception.getErrorCode());
        }
    }

    private record RotationResult(
            IssuedTokenPair tokenPair,
            AuthErrorCode errorCode
    ) {

        static RotationResult success(IssuedTokenPair tokenPair) {
            return new RotationResult(tokenPair, null);
        }

        static RotationResult failure(AuthErrorCode errorCode) {
            return new RotationResult(null, errorCode);
        }

        boolean isSuccess() {
            return tokenPair != null;
        }
    }
}
