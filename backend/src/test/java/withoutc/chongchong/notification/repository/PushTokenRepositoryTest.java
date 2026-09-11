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
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Transactional
class PushTokenRepositoryTest extends PostgresContainerTest {

    private static final String TOKEN = "push-token";
    private static final String INSTALLATION_ID = "installation-1";

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사용자와 설치 식별자, Provider, 플랫폼, 토큰을 저장하고 조회한다")
    void saveAndFindPushToken() {
        User user = saveUser("총총이");

        PushToken saved = pushTokenRepository.saveAndFlush(
                PushToken.create(user, INSTALLATION_ID, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID)
        );
        entityManager.clear();

        PushToken found = pushTokenRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(found.getToken()).isEqualTo(TOKEN);
        assertThat(found.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(found.getProvider()).isEqualTo(TokenProvider.EXPO);
        assertThat(found.isActive()).isTrue();
        assertThat(Hibernate.isInitialized(found.getUser())).isFalse();
        assertThat(countRowsWithAuditingTimestamps(saved.getId())).isOne();
    }

    @Test
    @DisplayName("같은 설치 식별자를 중복 저장하지 않는다")
    void rejectDuplicateInstallationId() {
        User user = saveUser("사용자");
        pushTokenRepository.saveAndFlush(
                PushToken.create(user, INSTALLATION_ID, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID)
        );
        PushToken duplicate = PushToken.create(
                user,
                INSTALLATION_ID,
                TokenProvider.EXPO,
                TOKEN,
                DevicePlatform.IOS
        );

        assertThatThrownBy(() -> pushTokenRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("사용자가 달라도 같은 설치 식별자를 저장할 수 없다")
    void rejectSameInstallationIdFromDifferentUsers() {
        User firstUser = saveUser("첫 번째 사용자");
        User secondUser = saveUser("두 번째 사용자");

        pushTokenRepository.saveAndFlush(
                PushToken.create(firstUser, INSTALLATION_ID, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID)
        );

        assertThatThrownBy(() -> pushTokenRepository.saveAndFlush(
                PushToken.create(secondUser, INSTALLATION_ID, TokenProvider.FCM, TOKEN, DevicePlatform.ANDROID)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("upsert하면 같은 설치 식별자의 사용자와 등록 정보를 모두 갱신한다")
    void upsertUpdatesRegistration() {
        User firstUser = saveUser("첫 번째 사용자");
        User secondUser = saveUser("두 번째 사용자");

        pushTokenRepository.upsert(
                firstUser.getId(), INSTALLATION_ID, TokenProvider.EXPO.name(), "old-token", "ANDROID"
        );
        pushTokenRepository.upsert(
                secondUser.getId(), INSTALLATION_ID, TokenProvider.EXPO.name(), "new-token", "IOS"
        );

        PushTokenRow saved = findPushToken();
        assertThat(pushTokenRepository.count()).isOne();
        assertThat(saved.userId()).isEqualTo(secondUser.getId());
        assertThat(saved.installationId()).isEqualTo(INSTALLATION_ID);
        assertThat(saved.provider()).isEqualTo(TokenProvider.EXPO.name());
        assertThat(saved.token()).isEqualTo("new-token");
        assertThat(saved.platform()).isEqualTo(DevicePlatform.IOS.name());
        assertThat(saved.active()).isTrue();
    }

    @Test
    @DisplayName("upsert하면 비활성화된 설치 식별자를 다시 활성화한다")
    void upsertReactivatesPushToken() {
        User user = saveUser("사용자");
        PushToken pushToken = pushTokenRepository.saveAndFlush(
                PushToken.create(user, INSTALLATION_ID, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID)
        );
        pushToken.deactivate();
        entityManager.flush();
        entityManager.clear();

        pushTokenRepository.upsert(
                user.getId(), INSTALLATION_ID, TokenProvider.EXPO.name(), TOKEN, DevicePlatform.ANDROID.name()
        );

        assertThat(findPushToken().active()).isTrue();
    }

    @Test
    @DisplayName("비활성화는 사용자와 설치 식별자가 일치하는 행만 변경한다")
    void deactivatePushTokenByUserAndInstallationId() {
        User user = saveUser("사용자");
        pushTokenRepository.upsert(
                user.getId(), INSTALLATION_ID, TokenProvider.EXPO.name(), TOKEN, DevicePlatform.ANDROID.name()
        );

        pushTokenRepository.deactivateByInstallationIdAndUserId("another-installation", user.getId());

        assertThat(findPushToken().active()).isTrue();

        pushTokenRepository.deactivateByInstallationIdAndUserId(INSTALLATION_ID, user.getId());

        assertThat(findPushToken().active()).isFalse();
    }

    private User saveUser(String name) {
        return userRepository.saveAndFlush(User.create(name, null));
    }

    private Integer countRowsWithAuditingTimestamps(Long pushTokenId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM push_tokens
                WHERE id = ?
                  AND created_at IS NOT NULL
                  AND updated_at IS NOT NULL
                """, Integer.class, pushTokenId);
    }

    private PushTokenRow findPushToken() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT user_id, installation_id, provider, token, platform, is_active
                        FROM push_tokens
                        """,
                (resultSet, rowNumber) -> new PushTokenRow(
                        resultSet.getLong("user_id"),
                        resultSet.getString("installation_id"),
                        resultSet.getString("provider"),
                        resultSet.getString("token"),
                        resultSet.getString("platform"),
                        resultSet.getBoolean("is_active")
                )
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
