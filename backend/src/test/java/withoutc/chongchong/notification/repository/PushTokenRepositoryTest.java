package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class PushTokenRepositoryTest {

    private static final String TOKEN = "push-token";

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사용자와 Provider, 플랫폼, 토큰을 저장하고 조회한다")
    void saveAndFindPushToken() {
        User user = saveUser("총총이");

        PushToken saved = pushTokenRepository.saveAndFlush(
                PushToken.create(user, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID)
        );
        entityManager.clear();

        PushToken found = pushTokenRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getToken()).isEqualTo(TOKEN);
        assertThat(found.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(found.getProvider()).isEqualTo(TokenProvider.EXPO);
        assertThat(found.isActive()).isTrue();
        assertThat(Hibernate.isInitialized(found.getUser())).isFalse();
        assertThat(countRowsWithAuditingTimestamps(saved.getId())).isOne();
    }

    @Test
    @DisplayName("같은 Provider와 토큰을 중복 저장하지 않는다")
    void rejectDuplicateProviderToken() {
        User firstUser = saveUser("첫 번째 사용자");
        User secondUser = saveUser("두 번째 사용자");
        pushTokenRepository.saveAndFlush(
                PushToken.create(firstUser, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID)
        );
        PushToken duplicate = PushToken.create(
                secondUser,
                TokenProvider.EXPO,
                TOKEN,
                DevicePlatform.IOS
        );

        assertThatThrownBy(() -> pushTokenRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Provider가 다르면 같은 토큰을 저장할 수 있다")
    void allowSameTokenFromDifferentProviders() {
        User expoUser = saveUser("Expo 사용자");
        User fcmUser = saveUser("FCM 사용자");

        pushTokenRepository.saveAndFlush(
                PushToken.create(expoUser, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID)
        );
        pushTokenRepository.saveAndFlush(
                PushToken.create(fcmUser, TokenProvider.FCM, TOKEN, DevicePlatform.ANDROID)
        );

        assertThat(pushTokenRepository.count()).isEqualTo(2);
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
}
