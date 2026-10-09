package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
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
    private static final String INSTALLATION_ID = "4c2f0b3f-0a57-4a37-bb15-8ad7f4f3c2a5";
    private static final String P256DH = "p256dh-key";
    private static final String AUTH = "auth-secret";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 12, 0);

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
                WebPushSubscription.create(user, INSTALLATION_ID, ENDPOINT, P256DH, AUTH)
        );
        entityManager.clear();

        WebPushSubscription found = webPushSubscriptionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(found.getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(found.getP256dh()).isEqualTo(P256DH);
        assertThat(found.getAuth()).isEqualTo(AUTH);
        assertThat(found.isActive()).isTrue();
        assertThat(Hibernate.isInitialized(found.getUser())).isFalse();
        assertThat(countRowsWithAuditingTimestamps(saved.getId())).isOne();
    }

    @Test
    @DisplayName("같은 사용자와 installationId를 중복 저장할 수 없다")
    void rejectDuplicateUserInstallation() {
        User user = saveUser("사용자");
        webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, INSTALLATION_ID, ENDPOINT, P256DH, AUTH)
        );

        assertThatThrownBy(() -> webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(
                        user, INSTALLATION_ID, ENDPOINT + "-another-installation",
                        "another-p256dh", "another-auth"
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("upsert하면 같은 사용자 installationId의 endpoint와 암호화 키를 갱신한다")
    void upsertUpdatesRegistration() {
        User user = saveUser("사용자");

        webPushSubscriptionRepository.upsert(user.getId(), INSTALLATION_ID, ENDPOINT, P256DH, AUTH, NOW);
        webPushSubscriptionRepository.upsert(
                user.getId(), INSTALLATION_ID, ENDPOINT, "new-p256dh", "new-auth", NOW
        );

        WebPushSubscription saved = webPushSubscriptionRepository
                .findByUserIdAndInstallationId(user.getId(), INSTALLATION_ID)
                .orElseThrow();
        assertThat(webPushSubscriptionRepository.count()).isOne();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getP256dh()).isEqualTo("new-p256dh");
        assertThat(saved.getAuth()).isEqualTo("new-auth");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("같은 installationId를 다른 사용자가 upsert하면 별도 구독을 저장한다")
    void upsertSameInstallationForAnotherUser() {
        User owner = saveUser("소유자");
        User otherUser = saveUser("다른 사용자");

        webPushSubscriptionRepository.upsert(owner.getId(), INSTALLATION_ID, ENDPOINT, P256DH, AUTH, NOW);

        int affectedRows = webPushSubscriptionRepository.upsert(
                otherUser.getId(), INSTALLATION_ID, ENDPOINT, "new-p256dh", "new-auth", NOW
        );

        WebPushSubscription ownerSubscription = webPushSubscriptionRepository
                .findByUserIdAndInstallationId(owner.getId(), INSTALLATION_ID)
                .orElseThrow();
        WebPushSubscription otherSubscription = webPushSubscriptionRepository
                .findByUserIdAndInstallationId(otherUser.getId(), INSTALLATION_ID)
                .orElseThrow();
        assertThat(affectedRows).isOne();
        assertThat(ownerSubscription.getP256dh()).isEqualTo(P256DH);
        assertThat(ownerSubscription.getAuth()).isEqualTo(AUTH);
        assertThat(otherSubscription.getP256dh()).isEqualTo("new-p256dh");
        assertThat(otherSubscription.getAuth()).isEqualTo("new-auth");
    }

    @Test
    @DisplayName("다른 사용자의 같은 installationId 또는 endpoint 활성 구독을 비활성화한다")
    void deactivateConflictingActiveSubscriptions() {
        User installationOwner = saveUser("installation 소유자");
        User endpointOwner = saveUser("endpoint 소유자");
        User currentUser = saveUser("현재 사용자");
        String otherInstallationId = "4c2f0b3f-0a57-4a37-bb15-8ad7f4f3c2af";

        WebPushSubscription sameInstallation = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(installationOwner, INSTALLATION_ID, ENDPOINT + "-installation",
                        P256DH, AUTH)
        );
        WebPushSubscription sameEndpoint = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(endpointOwner, otherInstallationId, ENDPOINT, P256DH, AUTH)
        );

        int deactivatedCount = webPushSubscriptionRepository.deactivateConflictingActiveSubscriptions(
                currentUser.getId(), INSTALLATION_ID, ENDPOINT, NOW
        );

        assertThat(deactivatedCount).isEqualTo(2);
        assertThat(webPushSubscriptionRepository.findById(sameInstallation.getId()).orElseThrow().isActive())
                .isFalse();
        assertThat(webPushSubscriptionRepository.findById(sameEndpoint.getId()).orElseThrow().isActive())
                .isFalse();
    }

    @Test
    @DisplayName("upsert하면 비활성화된 endpoint를 다시 활성화한다")
    void upsertReactivatesSubscription() {
        User user = saveUser("사용자");
        WebPushSubscription subscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, INSTALLATION_ID, ENDPOINT, P256DH, AUTH)
        );
        subscription.deactivate();
        entityManager.flush();
        entityManager.clear();

        webPushSubscriptionRepository.upsert(user.getId(), INSTALLATION_ID, ENDPOINT, P256DH, AUTH, NOW);

        assertThat(webPushSubscriptionRepository
                .findByUserIdAndInstallationId(user.getId(), INSTALLATION_ID)
                .orElseThrow()
                .isActive()).isTrue();
    }

    @Test
    @DisplayName("비활성화는 사용자와 구독 ID가 일치하는 행만 변경한다")
    void deactivateByUserAndId() {
        User user = saveUser("사용자");
        WebPushSubscription subscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(user, INSTALLATION_ID, ENDPOINT, P256DH, AUTH)
        );

        webPushSubscriptionRepository.deactivateByIdAndUserId(subscription.getId() + 1, user.getId(), NOW);
        assertThat(webPushSubscriptionRepository.findById(subscription.getId()).orElseThrow().isActive()).isTrue();

        webPushSubscriptionRepository.deactivateByIdAndUserId(subscription.getId(), user.getId(), NOW);
        assertThat(webPushSubscriptionRepository.findById(subscription.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("사용자의 활성 Web Push 구독만 조회한다")
    void findActiveSubscriptionsByUserId() {
        User user = saveUser("사용자");
        User anotherUser = saveUser("다른 사용자");
        WebPushSubscription activeSubscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(
                        user, "4c2f0b3f-0a57-4a37-bb15-8ad7f4f3c2a7", ENDPOINT + "-active", P256DH, AUTH
                )
        );
        WebPushSubscription inactiveSubscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(
                        user, "4c2f0b3f-0a57-4a37-bb15-8ad7f4f3c2a8", ENDPOINT + "-inactive", P256DH, AUTH
                )
        );
        webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(
                        anotherUser, "4c2f0b3f-0a57-4a37-bb15-8ad7f4f3c2a9", ENDPOINT + "-another-user",
                        P256DH, AUTH
                )
        );
        inactiveSubscription.deactivate();
        entityManager.flush();
        entityManager.clear();

        List<WebPushSubscription> activeSubscriptions = webPushSubscriptionRepository
                .findByUserIdAndIsActiveTrue(user.getId());

        assertThat(activeSubscriptions)
                .extracting(WebPushSubscription::getEndpoint)
                .containsExactly(activeSubscription.getEndpoint());
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
