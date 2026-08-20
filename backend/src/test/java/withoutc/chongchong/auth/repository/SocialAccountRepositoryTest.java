package withoutc.chongchong.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.auth.entity.SocialAccount;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SocialAccountRepositoryTest {

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사용자와 제공자 식별 정보를 저장하고 조회한다")
    void saveAndFindSocialAccount() {
        User user = saveUser("총총이");
        String providerUserId = " Provider-User-123 ";
        SocialAccount saved = socialAccountRepository.saveAndFlush(
                SocialAccount.create(user, SocialProvider.GOOGLE, providerUserId)
        );
        entityManager.clear();

        SocialAccount found = socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.GOOGLE,
                providerUserId
        ).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(found.getProviderUserId()).isEqualTo(providerUserId);
        assertThat(Hibernate.isInitialized(found.getUser())).isFalse();
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(countRowsWithAuditingTimestamps(saved.getId())).isOne();
    }

    @Test
    @DisplayName("같은 제공자의 같은 사용자 ID를 중복 저장하지 않는다")
    void rejectDuplicateProviderUser() {
        User firstUser = saveUser("첫 번째 사용자");
        User secondUser = saveUser("두 번째 사용자");
        socialAccountRepository.saveAndFlush(SocialAccount.create(
                firstUser,
                SocialProvider.GOOGLE,
                "same-provider-user-id"
        ));
        SocialAccount duplicate = SocialAccount.create(
                secondUser,
                SocialProvider.GOOGLE,
                "same-provider-user-id"
        );

        assertThatThrownBy(() -> socialAccountRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("제공자가 다르면 같은 제공자 사용자 ID를 각각 저장할 수 있다")
    void allowSameProviderUserIdFromDifferentProviders() {
        User googleUser = saveUser("Google 사용자");
        User kakaoUser = saveUser("Kakao 사용자");
        String providerUserId = "same-provider-user-id";

        socialAccountRepository.saveAndFlush(SocialAccount.create(
                googleUser,
                SocialProvider.GOOGLE,
                providerUserId
        ));
        socialAccountRepository.saveAndFlush(SocialAccount.create(
                kakaoUser,
                SocialProvider.KAKAO,
                providerUserId
        ));

        assertThat(socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.GOOGLE,
                providerUserId
        )).isPresent();
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                providerUserId
        )).isPresent();
        assertThat(socialAccountRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("한 사용자는 여러 제공자의 소셜 계정을 가질 수 있다")
    void allowMultipleProvidersForOneUser() {
        User user = saveUser("총총이");

        socialAccountRepository.save(SocialAccount.create(
                user,
                SocialProvider.GOOGLE,
                "google-user-id"
        ));
        socialAccountRepository.save(SocialAccount.create(
                user,
                SocialProvider.KAKAO,
                "kakao-user-id"
        ));
        socialAccountRepository.flush();

        assertThat(socialAccountRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("소셜 계정 식별 컬럼의 길이와 NULL 제약을 정의한다")
    void defineSocialAccountColumnConstraints() {
        Map<String, Object> providerColumn = findColumnMetadata("PROVIDER");
        Map<String, Object> providerUserIdColumn = findColumnMetadata("PROVIDER_USER_ID");
        Map<String, Object> userIdColumn = findColumnMetadata("USER_ID");

        assertThat(providerColumn.get("IS_NULLABLE")).isEqualTo("NO");
        assertThat(providerUserIdColumn.get("CHARACTER_MAXIMUM_LENGTH")).isEqualTo(255L);
        assertThat(providerUserIdColumn.get("IS_NULLABLE")).isEqualTo("NO");
        assertThat(userIdColumn.get("IS_NULLABLE")).isEqualTo("NO");
    }

    private User saveUser(String name) {
        return userRepository.saveAndFlush(User.create(name, null));
    }

    private Integer countRowsWithAuditingTimestamps(Long socialAccountId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM social_accounts
                WHERE id = ?
                  AND created_at IS NOT NULL
                  AND updated_at IS NOT NULL
                """, Integer.class, socialAccountId);
    }

    private Map<String, Object> findColumnMetadata(String columnName) {
        return jdbcTemplate.queryForMap("""
                SELECT CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = 'SOCIAL_ACCOUNTS'
                  AND COLUMN_NAME = ?
                """, columnName);
    }
}
