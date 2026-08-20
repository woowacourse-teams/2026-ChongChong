package withoutc.chongchong.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SocialLoginCommandTest {

    @Test
    @DisplayName("소셜 로그인 제공자와 Authorization Code를 보관한다")
    void createSocialLoginCommand() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "authorization-code"
        );

        assertThat(command.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(command.authorizationCode()).isEqualTo("authorization-code");
    }

    @Test
    @DisplayName("소셜 로그인 요청 문자열에 Authorization Code를 노출하지 않는다")
    void redactAuthorizationCodeFromToString() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "sensitive-authorization-code"
        );

        assertThat(command.toString())
                .contains("provider=GOOGLE")
                .contains("authorizationCode=REDACTED")
                .doesNotContain("sensitive-authorization-code");
    }

    @Test
    @DisplayName("소셜 로그인 제공자는 필수다")
    void rejectNullProvider() {
        assertThatThrownBy(() -> new SocialLoginCommand(null, "authorization-code"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인 제공자는 필수입니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("Authorization Code가 비어 있으면 소셜 로그인 요청을 만들 수 없다")
    void rejectBlankAuthorizationCode(String authorizationCode) {
        assertThatThrownBy(() -> new SocialLoginCommand(
                SocialProvider.GOOGLE,
                authorizationCode
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authorization Code는 비어 있을 수 없습니다.");
    }
}
