package withoutc.chongchong.auth.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenRefreshResponseTest {

    @Test
    @DisplayName("갱신 응답 문자열에 Access Token을 노출하지 않는다")
    void redactAccessTokenFromStringRepresentation() {
        AccessTokenRefreshResponse response = new AccessTokenRefreshResponse(
                "Bearer",
                "sensitive-access-token",
                Instant.parse("2026-08-21T01:00:00Z")
        );

        assertThat(response.toString())
                .startsWith("AccessTokenRefreshResponse[")
                .contains("accessToken=REDACTED")
                .doesNotContain("sensitive-access-token")
                .doesNotContain("refreshToken");
    }
}