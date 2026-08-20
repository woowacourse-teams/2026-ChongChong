package withoutc.chongchong.auth.token;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class IssuedAccessTokenTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-08-20T00:30:00Z");

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("비어 있는 Access Token 값을 허용하지 않는다")
    void rejectBlankValue(String value) {
        assertThatThrownBy(() -> new IssuedAccessToken(value, EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("발급된 Access Token 값은 필수입니다.");
    }

    @Test
    @DisplayName("Access Token 만료 시각은 필수다")
    void rejectNullExpiresAt() {
        assertThatThrownBy(() -> new IssuedAccessToken("access-token", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access Token 만료 시각은 필수입니다.");
    }
}
