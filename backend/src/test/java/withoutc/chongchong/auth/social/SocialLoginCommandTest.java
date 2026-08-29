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
    @DisplayName("소셜 로그인 제공자와 인증 정보를 보관한다")
    void createSocialLoginCommand() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "credential"
        );

        assertThat(command.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(command.credential()).isEqualTo("credential");
    }

    @Test
    @DisplayName("소셜 로그인 요청 문자열에 인증 정보를 노출하지 않는다")
    void redactCredentialFromToString() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "sensitive-credential"
        );

        assertThat(command.toString())
                .contains("provider=GOOGLE")
                .contains("credential=REDACTED")
                .doesNotContain("sensitive-credential");
    }

    @Test
    @DisplayName("소셜 로그인 제공자는 필수다")
    void rejectNullProvider() {
        assertThatThrownBy(() -> new SocialLoginCommand(null, "credential"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인 제공자는 필수입니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("인증 정보가 비어 있으면 소셜 로그인 요청을 만들 수 없다")
    void rejectBlankCredential(String credential) {
        assertThatThrownBy(() -> new SocialLoginCommand(
                SocialProvider.GOOGLE,
                credential
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소셜 로그인 인증 정보는 비어 있을 수 없습니다.");
    }
}
