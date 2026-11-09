package withoutc.chongchong.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.auth.social.SocialProvider;

class SocialLoginRequestTest {

    @Test
    @DisplayName("로그인 요청 문자열에 Kakao 인가 코드를 노출하지 않는다")
    void redactAuthorizationCodeFromToString() {
        SocialLoginRequest request = new SocialLoginRequest(
                SocialProvider.KAKAO,
                "sensitive-kakao-authorization-code"
        );

        assertThat(request.toString())
                .contains("provider=KAKAO")
                .contains("authorizationCode=REDACTED")
                .doesNotContain("sensitive-kakao-authorization-code");
    }
}
