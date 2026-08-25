package withoutc.chongchong.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SocialUserInfoTest {

    @Test
    @DisplayName("Provider가 검증한 사용자 정보를 원문 그대로 보관한다")
    void createSocialUserInfo() {
        SocialUserInfo socialUserInfo = new SocialUserInfo(
                SocialProvider.GOOGLE,
                " Provider-User-123 ",
                " 총총이 ",
                "https://example.com/profile.png"
        );

        assertThat(socialUserInfo.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(socialUserInfo.providerUserId()).isEqualTo(" Provider-User-123 ");
        assertThat(socialUserInfo.displayName()).isEqualTo(" 총총이 ");
        assertThat(socialUserInfo.profileImageUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    @DisplayName("프로필 이미지 URL은 없을 수 있다")
    void allowNullProfileImageUrl() {
        SocialUserInfo socialUserInfo = new SocialUserInfo(
                SocialProvider.GOOGLE,
                "provider-user-id",
                "총총이",
                null
        );

        assertThat(socialUserInfo.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("검증 결과에는 Provider Token과 인증 정보가 포함되지 않는다")
    void containOnlyVerifiedUserInformation() {
        assertThat(Arrays.stream(SocialUserInfo.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly("provider", "providerUserId", "displayName", "profileImage");
    }

    @Test
    @DisplayName("소셜 로그인 제공자는 필수다")
    void rejectNullProvider() {
        assertThatThrownBy(() -> new SocialUserInfo(
                null,
                "provider-user-id",
                "총총이",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인 제공자는 필수입니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("제공자 사용자 ID가 비어 있으면 검증 결과를 만들 수 없다")
    void rejectBlankProviderUserId(String providerUserId) {
        assertThatThrownBy(() -> new SocialUserInfo(
                SocialProvider.GOOGLE,
                providerUserId,
                "총총이",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 제공자 사용자 ID는 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("제공자 사용자 ID가 255자를 초과하면 검증 결과를 만들 수 없다")
    void rejectTooLongProviderUserId() {
        assertThatThrownBy(() -> new SocialUserInfo(
                SocialProvider.GOOGLE,
                "a".repeat(256),
                "총총이",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 제공자 사용자 ID는 255자를 초과할 수 없습니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("사용자 이름이 비어 있으면 검증 결과를 만들 수 없다")
    void rejectBlankDisplayName(String displayName) {
        assertThatThrownBy(() -> new SocialUserInfo(
                SocialProvider.GOOGLE,
                "provider-user-id",
                displayName,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 사용자 이름은 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("사용자 이름이 255자를 초과하면 검증 결과를 만들 수 없다")
    void rejectTooLongDisplayName() {
        assertThatThrownBy(() -> new SocialUserInfo(
                SocialProvider.GOOGLE,
                "provider-user-id",
                "가".repeat(256),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 사용자 이름은 255자를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("프로필 이미지 URL이 공백이면 검증 결과를 만들 수 없다")
    void rejectBlankProfileImageUrl() {
        assertThatThrownBy(() -> new SocialUserInfo(
                SocialProvider.GOOGLE,
                "provider-user-id",
                "총총이",
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("프로필 이미지 URL은 공백일 수 없습니다.");
    }

    @Test
    @DisplayName("프로필 이미지 URL이 2048자를 초과하면 검증 결과를 만들 수 없다")
    void rejectTooLongProfileImageUrl() {
        assertThatThrownBy(() -> new SocialUserInfo(
                SocialProvider.GOOGLE,
                "provider-user-id",
                "총총이",
                "a".repeat(2049)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("프로필 이미지 URL은 2048자를 초과할 수 없습니다.");
    }
}
