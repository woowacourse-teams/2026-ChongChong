package withoutc.chongchong.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.auth.token.IssuedAccessToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;

class SocialLoginResponseTest {

    @Test
    @DisplayName("발급한 Token 쌍에서 Access Token 정보만 로그인 응답으로 변환한다")
    void createResponseFromIssuedTokenPair() {
        Instant accessTokenExpiresAt = Instant.parse("2026-08-21T01:00:00Z");
        Instant refreshTokenExpiresAt = Instant.parse("2026-09-20T01:00:00Z");
        IssuedTokenPair tokenPair = new IssuedTokenPair(
                new IssuedAccessToken("access-token", accessTokenExpiresAt),
                new RawRefreshToken("refresh-token"),
                refreshTokenExpiresAt
        );

        SocialLoginResponse response = SocialLoginResponse.from(tokenPair);

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.accessTokenExpiresAt()).isEqualTo(accessTokenExpiresAt);
    }

    @Test
    @DisplayName("로그인 응답 문자열에 Access Token을 노출하지 않는다")
    void redactTokensFromToString() {
        SocialLoginResponse response = new SocialLoginResponse(
                "Bearer",
                "sensitive-access-token",
                Instant.parse("2026-08-21T01:00:00Z")
        );

        assertThat(response.toString())
                .contains("accessToken=REDACTED")
                .doesNotContain("sensitive-access-token")
                .doesNotContain("refreshToken");
    }
}
