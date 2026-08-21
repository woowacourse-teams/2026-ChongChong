package withoutc.chongchong.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;

class SocialLoginRequestTest {

    @Test
    @DisplayName("Google ID Token을 Provider 독립 인증 정보로 변환한다")
    void convertIdTokenToCredential() {
        SocialLoginRequest request = new SocialLoginRequest(
                SocialProvider.GOOGLE,
                "google-id-token"
        );

        SocialLoginCommand command = request.toCommand();

        assertThat(command.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(command.credential()).isEqualTo("google-id-token");
    }

    @Test
    @DisplayName("로그인 요청 문자열에 Google ID Token을 노출하지 않는다")
    void redactIdTokenFromToString() {
        SocialLoginRequest request = new SocialLoginRequest(
                SocialProvider.GOOGLE,
                "sensitive-google-id-token"
        );

        assertThat(request.toString())
                .contains("provider=GOOGLE")
                .contains("idToken=REDACTED")
                .doesNotContain("sensitive-google-id-token");
    }
}
