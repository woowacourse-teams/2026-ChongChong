package withoutc.chongchong.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenPropertiesTest {

    @Test
    @DisplayName("양수인 Refresh Token 유효 시간을 허용한다")
    void allowPositiveValidity() {
        RefreshTokenProperties properties = new RefreshTokenProperties(Duration.ofDays(30));

        assertThat(properties.validity()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    @DisplayName("누락된 Refresh Token 유효 시간을 거부한다")
    void rejectNullValidity() {
        assertThatThrownBy(() -> new RefreshTokenProperties(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token 유효 시간은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("0인 Refresh Token 유효 시간을 거부한다")
    void rejectZeroValidity() {
        assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token 유효 시간은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("음수인 Refresh Token 유효 시간을 거부한다")
    void rejectNegativeValidity() {
        assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token 유효 시간은 0보다 커야 합니다.");
    }
}
