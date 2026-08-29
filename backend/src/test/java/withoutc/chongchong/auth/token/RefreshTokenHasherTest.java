package withoutc.chongchong.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();

    @Test
    @DisplayName("Refresh Token 원문을 SHA-256 소문자 16진수로 해시한다")
    void hashRefreshTokenWithSha256() {
        HashedRefreshToken hashedRefreshToken = refreshTokenHasher.hash(new RawRefreshToken("abc"));

        assertThat(hashedRefreshToken.value())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                .matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("같은 Refresh Token 원문은 같은 해시를 만든다")
    void hashSameRawRefreshTokenDeterministically() {
        RawRefreshToken rawRefreshToken = new RawRefreshToken("same-refresh-token");

        HashedRefreshToken first = refreshTokenHasher.hash(rawRefreshToken);
        HashedRefreshToken second = refreshTokenHasher.hash(rawRefreshToken);

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("다른 Refresh Token 원문은 다른 해시를 만든다")
    void hashDifferentRawRefreshTokensDifferently() {
        HashedRefreshToken first = refreshTokenHasher.hash(new RawRefreshToken("first-refresh-token"));
        HashedRefreshToken second = refreshTokenHasher.hash(new RawRefreshToken("second-refresh-token"));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Refresh Token 해시를 문자열 표현에 노출하지 않는다")
    void hideRefreshTokenHashFromStringRepresentation() {
        HashedRefreshToken hashedRefreshToken = refreshTokenHasher.hash(new RawRefreshToken("refresh-token"));

        assertThat(hashedRefreshToken.toString())
                .doesNotContain(hashedRefreshToken.value())
                .contains("REDACTED");
    }

    @Test
    @DisplayName("Refresh Token 원문 없이 해시하지 않는다")
    void rejectNullRawRefreshToken() {
        assertThatThrownBy(() -> refreshTokenHasher.hash(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token 원문은 필수입니다.");
    }

    @Test
    @DisplayName("SHA-256 형식이 아닌 Refresh Token 해시를 허용하지 않는다")
    void rejectInvalidRefreshTokenHash() {
        assertThatThrownBy(() -> new HashedRefreshToken("invalid-hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token 해시는 64자리 소문자 16진수여야 합니다.");
    }
}
