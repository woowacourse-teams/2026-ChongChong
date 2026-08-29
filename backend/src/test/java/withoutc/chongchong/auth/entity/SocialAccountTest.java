package withoutc.chongchong.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.user.entity.User;

class SocialAccountTest {

    private static final User USER = User.create("총총이", null);

    @Test
    @DisplayName("소셜 계정은 사용자와 제공자 식별 정보를 그대로 보관한다")
    void createSocialAccount() {
        String providerUserId = " Provider-User-123 ";

        SocialAccount socialAccount = SocialAccount.create(
                USER,
                SocialProvider.GOOGLE,
                providerUserId
        );

        assertThat(socialAccount.getUser()).isSameAs(USER);
        assertThat(socialAccount.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(socialAccount.getProviderUserId()).isEqualTo(providerUserId);
    }

    @Test
    @DisplayName("사용자가 없으면 소셜 계정을 생성할 수 없다")
    void rejectNullUser() {
        assertThatThrownBy(() -> SocialAccount.create(
                null,
                SocialProvider.GOOGLE,
                "provider-user-id"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 계정의 사용자는 필수입니다.");
    }

    @Test
    @DisplayName("제공자가 없으면 소셜 계정을 생성할 수 없다")
    void rejectNullProvider() {
        assertThatThrownBy(() -> SocialAccount.create(
                USER,
                null,
                "provider-user-id"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인 제공자는 필수입니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("제공자 사용자 ID가 비어 있으면 소셜 계정을 생성할 수 없다")
    void rejectBlankProviderUserId(String providerUserId) {
        assertThatThrownBy(() -> SocialAccount.create(
                USER,
                SocialProvider.GOOGLE,
                providerUserId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 제공자 사용자 ID는 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("제공자 사용자 ID가 255자를 초과하면 소셜 계정을 생성할 수 없다")
    void rejectTooLongProviderUserId() {
        assertThatThrownBy(() -> SocialAccount.create(
                USER,
                SocialProvider.GOOGLE,
                "a".repeat(256)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 제공자 사용자 ID는 255자를 초과할 수 없습니다.");
    }
}
