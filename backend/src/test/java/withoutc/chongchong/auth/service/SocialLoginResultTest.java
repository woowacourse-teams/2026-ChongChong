package withoutc.chongchong.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.auth.token.IssuedAccessToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SocialLoginResultTest {

    @Test
    @DisplayName("사용자 ID와 발급 Token으로 소셜 로그인 결과를 생성한다")
    void createSocialLoginResult() {
        IssuedTokenPair tokenPair = createIssuedTokenPair();

        SocialLoginResult result = SocialLoginResult.of(1L, tokenPair);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.tokenPair()).isSameAs(tokenPair);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("사용자 ID가 없거나 양수가 아니면 로그인 결과를 생성하지 않는다")
    void rejectInvalidUserId(Long userId) {
        IssuedTokenPair tokenPair = createIssuedTokenPair();

        assertThatThrownBy(() -> SocialLoginResult.of(userId, tokenPair))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인 사용자 ID는 양수여야 합니다.");
    }

    @Test
    @DisplayName("발급 Token이 없으면 로그인 결과를 생성하지 않는다")
    void rejectMissingTokenPair() {
        assertThatThrownBy(() -> SocialLoginResult.of(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인 발급 Token은 필수입니다.");
    }

    private IssuedTokenPair createIssuedTokenPair() {
        return new IssuedTokenPair(
                new IssuedAccessToken(
                        "access-token",
                        Instant.parse("2026-09-23T01:00:00Z")
                ),
                new RawRefreshToken("refresh-token"),
                Instant.parse("2026-10-23T00:00:00Z")
        );
    }
}