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
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionKeysRequest;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterRequest;
import withoutc.chongchong.notification.service.WebPushSubscriptionService;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class WebPushSubscriptionConcurrencyTest extends PostgresContainerTest {

    private static final String ENDPOINT = "https://push.example.com/same-subscription";

    @Autowired
    private WebPushSubscriptionService webPushSubscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 endpoint를 동시에 최초 등록해도 하나의 최신 구독으로 수렴한다")
    void concurrentFirstRegistrationsConvergeToOneSubscription() throws Exception {
        User user = userRepository.saveAndFlush(User.create("사용자", null));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Void>> registrations = List.of(
                    executor.submit(registration(barrier, user.getId(), "first-p256dh", "first-auth")),
                    executor.submit(registration(barrier, user.getId(), "second-p256dh", "second-auth"))
            );

            for (Future<Void> registration : registrations) {
                assertThatCode(() -> registration.get(10, SECONDS))
                        .doesNotThrowAnyException();
            }
        } finally {
            executor.shutdownNow();
        }

        WebPushSubscriptionRow saved = findSubscription();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM web_push_subscriptions WHERE endpoint = ?",
                Integer.class,
                ENDPOINT
        )).isOne();
        assertThat(saved.userId()).isEqualTo(user.getId());
        assertThat(saved.endpoint()).isEqualTo(ENDPOINT);
        if (saved.p256dh().equals("first-p256dh")) {
            assertThat(saved.p256dh()).isEqualTo("first-p256dh");
            assertThat(saved.auth()).isEqualTo("first-auth");
        } else {
            assertThat(saved.p256dh()).isEqualTo("second-p256dh");
            assertThat(saved.auth()).isEqualTo("second-auth");
        }
        assertThat(saved.active()).isTrue();
    }

    private Callable<Void> registration(
            CyclicBarrier barrier,
            Long userId,
            String p256dh,
            String auth
    ) {
        return () -> {
            barrier.await(10, SECONDS);
            webPushSubscriptionService.register(
                    userId,
                    new WebPushSubscriptionRegisterRequest(
                            ENDPOINT,
                            new WebPushSubscriptionKeysRequest(p256dh, auth)
                    )
            );
            return null;
        };
    }

    private WebPushSubscriptionRow findSubscription() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT user_id, endpoint, p256dh, auth, is_active
                        FROM web_push_subscriptions
                        WHERE endpoint = ?
                        """,
                (resultSet, rowNum) -> new WebPushSubscriptionRow(
                        resultSet.getLong("user_id"),
                        resultSet.getString("endpoint"),
                        resultSet.getString("p256dh"),
                        resultSet.getString("auth"),
                        resultSet.getBoolean("is_active")
                ),
                ENDPOINT
        );
    }

    private record WebPushSubscriptionRow(
            Long userId,
            String endpoint,
            String p256dh,
            String auth,
            boolean active
    ) {
    }
}
