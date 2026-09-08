package withoutc.chongchong.notification;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.notification.controller.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.notification.service.PushTokenService;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class PushTokenConcurrencyTest extends PostgresContainerTest {

    private static final String INSTALLATION_ID = "same-installation";

    @Autowired
    private PushTokenService pushTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 설치 식별자를 동시에 최초 등록해도 하나의 최신 등록으로 수렴한다")
    void concurrentFirstRegistrationsConvergeToOneRegistration() throws Exception {
        User firstUser = userRepository.saveAndFlush(User.create("첫 번째 사용자", null));
        User secondUser = userRepository.saveAndFlush(User.create("두 번째 사용자", null));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Void>> registrations = List.of(
                    executor.submit(registration(barrier, firstUser.getId(), "first-token", DevicePlatform.ANDROID)),
                    executor.submit(registration(barrier, secondUser.getId(), "second-token", DevicePlatform.IOS))
            );

            for (Future<Void> registration : registrations) {
                assertThatCode(() -> registration.get(10, SECONDS))
                        .doesNotThrowAnyException();
            }
        } finally {
            executor.shutdownNow();
        }

        PushTokenRow saved = findPushToken();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM push_tokens WHERE installation_id = ?",
                Integer.class,
                INSTALLATION_ID
        )).isOne();
        assertThat(List.of(firstUser.getId(), secondUser.getId())).contains(saved.userId());
        assertThat(saved.installationId()).isEqualTo(INSTALLATION_ID);
        assertThat(saved.provider()).isEqualTo(TokenProvider.EXPO.name());
        if (saved.userId().equals(firstUser.getId())) {
            assertThat(saved.token()).isEqualTo("first-token");
            assertThat(saved.platform()).isEqualTo(DevicePlatform.ANDROID.name());
        } else {
            assertThat(saved.token()).isEqualTo("second-token");
            assertThat(saved.platform()).isEqualTo(DevicePlatform.IOS.name());
        }
        assertThat(saved.active()).isTrue();
    }

    private Callable<Void> registration(
            CyclicBarrier barrier,
            Long userId,
            String token,
            DevicePlatform platform
    ) {
        return () -> {
            barrier.await(10, SECONDS);
            pushTokenService.registerPushToken(
                    userId,
                    new PushTokenCreateRequest(INSTALLATION_ID, token, platform)
            );
            return null;
        };
    }

    private PushTokenRow findPushToken() {
        return jdbcTemplate.queryForObject(
                """
                SELECT user_id, installation_id, provider, token, platform, is_active
                FROM push_tokens
                WHERE installation_id = ?
                """,
                (resultSet, rowNum) -> new PushTokenRow(
                        resultSet.getLong("user_id"),
                        resultSet.getString("installation_id"),
                        resultSet.getString("provider"),
                        resultSet.getString("token"),
                        resultSet.getString("platform"),
                        resultSet.getBoolean("is_active")
                ),
                INSTALLATION_ID
        );
    }

    private record PushTokenRow(
            Long userId,
            String installationId,
            String provider,
            String token,
            String platform,
            boolean active
    ) {
    }
}
