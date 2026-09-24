package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Transactional
class WebPushSubscriptionRepositoryTest extends PostgresContainerTest {

    private static final String ENDPOINT = "https://push.example.com/subscription";
    private static final String P256DH = "p256dh-key";
    private static final String AUTH = "auth-secret";

    @Autowired
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사용자와 Web Push 구독 정보를 저장하고 조회한다")
    void saveAndFindWebPushSubscription() {
        User user = saveUser("총총이");

        WebPushSubscription saved = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, ENDPOINT, P256DH, AUTH)
        );
        entityManager.clear();

        WebPushSubscription found = webPushSubscriptionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(found.getP256dh()).isEqualTo(P256DH);
        assertThat(found.getAuth()).isEqualTo(AUTH);
        assertThat(found.isActive()).isTrue();
        assertThat(Hibernate.isInitialized(found.getUser())).isFalse();
        assertThat(countRowsWithAuditingTimestamps(saved.getId())).isOne();
    }

    @Test
    @DisplayName("같은 endpoint를 중복 저장할 수 없다")
    void rejectDuplicateEndpoint() {
        User user = saveUser("사용자");
        webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, ENDPOINT, P256DH, AUTH)
        );

        assertThatThrownBy(() -> webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, ENDPOINT, "another-p256dh", "another-auth")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("upsert하면 같은 사용자 endpoint의 암호화 키를 갱신한다")
    void upsertUpdatesRegistration() {
        User user = saveUser("사용자");

        webPushSubscriptionRepository.upsert(user.getId(), ENDPOINT, P256DH, AUTH);
        webPushSubscriptionRepository.upsert(user.getId(), ENDPOINT, "new-p256dh", "new-auth");

        WebPushSubscription saved = webPushSubscriptionRepository.findByEndpoint(ENDPOINT).orElseThrow();
        assertThat(webPushSubscriptionRepository.count()).isOne();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getP256dh()).isEqualTo("new-p256dh");
        assertThat(saved.getAuth()).isEqualTo("new-auth");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자가 이미 등록한 endpoint는 upsert하지 않는다")
    void rejectUpsertForAnotherUser() {
        User owner = saveUser("소유자");
        User otherUser = saveUser("다른 사용자");

        webPushSubscriptionRepository.upsert(owner.getId(), ENDPOINT, P256DH, AUTH);

        int affectedRows = webPushSubscriptionRepository.upsert(
                otherUser.getId(), ENDPOINT, "new-p256dh", "new-auth"
        );

        WebPushSubscription saved = webPushSubscriptionRepository.findByEndpoint(ENDPOINT).orElseThrow();
        assertThat(affectedRows).isZero();
        assertThat(saved.getUser().getId()).isEqualTo(owner.getId());
        assertThat(saved.getP256dh()).isEqualTo(P256DH);
        assertThat(saved.getAuth()).isEqualTo(AUTH);
    }

    @Test
    @DisplayName("upsert하면 비활성화된 endpoint를 다시 활성화한다")
    void upsertReactivatesSubscription() {
        User user = saveUser("사용자");
        WebPushSubscription subscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, ENDPOINT, P256DH, AUTH)
        );
        subscription.deactivate();
        entityManager.flush();
        entityManager.clear();

        webPushSubscriptionRepository.upsert(user.getId(), ENDPOINT, P256DH, AUTH);

        assertThat(webPushSubscriptionRepository.findByEndpoint(ENDPOINT).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("비활성화는 사용자와 구독 ID가 일치하는 행만 변경한다")
    void deactivateByUserAndId() {
        User user = saveUser("사용자");
        WebPushSubscription subscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, ENDPOINT, P256DH, AUTH)
        );

        webPushSubscriptionRepository.deactivateByIdAndUserId(subscription.getId() + 1, user.getId());
        assertThat(webPushSubscriptionRepository.findById(subscription.getId()).orElseThrow().isActive()).isTrue();

        webPushSubscriptionRepository.deactivateByIdAndUserId(subscription.getId(), user.getId());
        assertThat(webPushSubscriptionRepository.findById(subscription.getId()).orElseThrow().isActive()).isFalse();
    }

    private User saveUser(String name) {
        return userRepository.saveAndFlush(User.create(name, null));
    }

    private Integer countRowsWithAuditingTimestamps(Long subscriptionId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM web_push_subscriptions
                WHERE id = ?
                  AND created_at IS NOT NULL
                  AND updated_at IS NOT NULL
                """, Integer.class, subscriptionId);
    }
}
