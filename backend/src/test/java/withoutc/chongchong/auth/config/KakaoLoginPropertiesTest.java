package withoutc.chongchong.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class KakaoLoginPropertiesTest {

    private static final URI REDIRECT_URI = URI.create("http://localhost:3005/auth/kakao/callback");
    private static final URI TOKEN_URI = URI.create("https://kauth.kakao.com/oauth/token");
    private static final URI USER_INFO_URI = URI.create("https://kapi.kakao.com/v2/user/me");

    @Test
    @DisplayName("Kakao 로그인에 필요한 설정을 보관한다")
    void holdValidProperties() {
        KakaoLoginProperties properties = properties("rest-api-key", "client-secret");

        assertThat(properties.restApiKey()).isEqualTo("rest-api-key");
        assertThat(properties.clientSecret()).isEqualTo("client-secret");
        assertThat(properties.redirectUri()).isEqualTo(REDIRECT_URI);
        assertThat(properties.tokenUri()).isEqualTo(TOKEN_URI);
        assertThat(properties.userInfoUri()).isEqualTo(USER_INFO_URI);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("비어 있는 Kakao REST API 키를 거부한다")
    void rejectBlankRestApiKey(String restApiKey) {
        assertThatThrownBy(() -> properties(restApiKey, "client-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao REST API 키는 비어 있을 수 없습니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("비어 있는 Kakao Client Secret을 거부한다")
    void rejectBlankClientSecret(String clientSecret) {
        assertThatThrownBy(() -> properties("rest-api-key", clientSecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao Client Secret은 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("누락되거나 안전한 HTTP 형식이 아닌 Kakao URI를 거부한다")
    void rejectInvalidUris() {
        assertThatThrownBy(() -> new KakaoLoginProperties(
                "rest-api-key",
                "client-secret",
                URI.create("/auth/kakao/callback"),
                TOKEN_URI,
                USER_INFO_URI
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao Redirect URI는 HTTPS 또는 loopback HTTP URI여야 합니다.");

        assertThatThrownBy(() -> new KakaoLoginProperties(
                "rest-api-key",
                "client-secret",
                REDIRECT_URI,
                URI.create("file:///tmp/kakao-token"),
                USER_INFO_URI
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao Token URI는 HTTPS 또는 loopback HTTP URI여야 합니다.");

        assertThatThrownBy(() -> new KakaoLoginProperties(
                "rest-api-key",
                "client-secret",
                REDIRECT_URI,
                TOKEN_URI,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao 사용자 정보 URI는 HTTPS 또는 loopback HTTP URI여야 합니다.");
    }

    @Test
    @DisplayName("외부 호스트로 평문 Kakao 자격 증명을 전송하는 URI를 거부한다")
    void rejectExternalHttpUri() {
        assertThatThrownBy(() -> new KakaoLoginProperties(
                "rest-api-key",
                "client-secret",
                REDIRECT_URI,
                URI.create("http://example.com/oauth/token"),
                USER_INFO_URI
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kakao Token URI는 HTTPS 또는 loopback HTTP URI여야 합니다.");
    }

    @Test
    @DisplayName("문자열 표현에 REST API 키와 Client Secret을 노출하지 않는다")
    void redactCredentialsFromString() {
        KakaoLoginProperties properties = properties("sensitive-rest-api-key", "sensitive-client-secret");

        assertThat(properties.toString())
                .doesNotContain("sensitive-rest-api-key", "sensitive-client-secret")
                .contains("restApiKey=REDACTED", "clientSecret=REDACTED");
    }

    private KakaoLoginProperties properties(String restApiKey, String clientSecret) {
        return new KakaoLoginProperties(
                restApiKey,
                clientSecret,
                REDIRECT_URI,
                TOKEN_URI,
                USER_INFO_URI
        );
    }
}
