package withoutc.chongchong.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator refreshTokenGenerator = new RefreshTokenGenerator(new SecureRandom());

    @Test
    @DisplayName("256비트 URL-safe Refresh Token을 padding 없이 생성한다")
    void generateUrlSafeRefreshToken() {
        RawRefreshToken rawRefreshToken = refreshTokenGenerator.generate();

        assertThat(rawRefreshToken.value()).matches("[A-Za-z0-9_-]{43}");
        assertThat(Base64.getUrlDecoder().decode(rawRefreshToken.value())).hasSize(32);
    }

    @Test
    @DisplayName("Refresh Token을 연속으로 생성하면 서로 다른 값을 만든다")
    void generateDifferentRefreshTokens() {
        RawRefreshToken first = refreshTokenGenerator.generate();
        RawRefreshToken second = refreshTokenGenerator.generate();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Refresh Token 원문을 문자열 표현에 노출하지 않는다")
    void hideRawRefreshTokenFromStringRepresentation() {
        RawRefreshToken rawRefreshToken = refreshTokenGenerator.generate();

        assertThat(rawRefreshToken.toString())
                .doesNotContain(rawRefreshToken.value())
                .contains("REDACTED");
    }

    @Test
    @DisplayName("비어 있는 Refresh Token 원문을 허용하지 않는다")
    void rejectBlankRawRefreshToken() {
        assertThatThrownBy(() -> new RawRefreshToken(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token 원문은 비어 있을 수 없습니다.");
    }
}
