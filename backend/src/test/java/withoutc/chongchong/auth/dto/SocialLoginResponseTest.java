package withoutc.chongchong.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialLoginResponseTest {

    @Test
    @DisplayName("로그인 응답 문자열에 Access Token을 노출하지 않는다")
    void redactAccessTokenFromStringRepresentation() {
        SocialLoginResponse response = new SocialLoginResponse(
                1L,
                "Bearer",
                "sensitive-access-token",
                Instant.parse("2026-08-21T01:00:00Z")
        );

        assertThat(response.toString())
                .startsWith("SocialLoginResponse[")
                .contains("userId=1")
                .contains("accessToken=REDACTED")
                .doesNotContain("sensitive-access-token")
                .doesNotContain("refreshToken");
    }
}
