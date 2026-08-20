package withoutc.chongchong.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import withoutc.chongchong.auth.config.JwtConfig;
import withoutc.chongchong.auth.config.JwtProperties;

class AccessTokenIssuerTest {

    private static final String ISSUER = "chongchong-test";
    private static final String AUDIENCE = "chongchong-test-api";
    private static final byte[] SECRET = "01234567890123456789012345678901".getBytes();
    private static final String ENCODED_SECRET = Base64.getEncoder().encodeToString(SECRET);
    private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(30);
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtProperties properties = new JwtProperties(
            ISSUER,
            AUDIENCE,
            ENCODED_SECRET,
            ACCESS_TOKEN_VALIDITY
    );
    private final SecretKey secretKey = jwtConfig.authJwtSecretKey(properties);
    private final JwtEncoder jwtEncoder = jwtConfig.jwtEncoder(secretKey);
    private final JwtDecoder jwtDecoder = jwtConfig.jwtDecoder(secretKey, properties);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AccessTokenIssuer accessTokenIssuer = new AccessTokenIssuer(jwtEncoder, properties, clock);

    @Test
    @DisplayName("기존 JwtDecoder가 검증할 수 있는 Access Token을 발급한다")
    void issueAccessTokenDecodableByProductionDecoder() {
        IssuedAccessToken issuedAccessToken = accessTokenIssuer.issue(1L);

        Jwt jwt = jwtDecoder.decode(issuedAccessToken.value());

        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(issuedAccessToken.expiresAt()).isEqualTo(NOW.plus(ACCESS_TOKEN_VALIDITY));
    }

    @Test
    @DisplayName("Access Token에 인증 경계에서 정한 표준 Claim만 포함한다")
    void includeExpectedClaims() {
        IssuedAccessToken issuedAccessToken = accessTokenIssuer.issue(42L);

        Jwt jwt = jwtDecoder.decode(issuedAccessToken.value());

        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(ACCESS_TOKEN_VALIDITY));
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaims().keySet())
                .containsExactlyInAnyOrderElementsOf(Set.of("iss", "aud", "sub", "iat", "exp", "jti"));
    }

    @Test
    @DisplayName("Access Token을 발급할 때마다 다른 jti를 사용한다")
    void issueUniqueJwtId() {
        Jwt first = jwtDecoder.decode(accessTokenIssuer.issue(1L).value());
        Jwt second = jwtDecoder.decode(accessTokenIssuer.issue(1L).value());

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("양수가 아닌 사용자 ID로 Access Token을 발급하지 않는다")
    void rejectInvalidUserId(Long userId) {
        assertThatThrownBy(() -> accessTokenIssuer.issue(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 양수여야 합니다.");
    }

    @Test
    @DisplayName("Access Token 결과를 문자열로 표현할 때 Token 원문을 노출하지 않는다")
    void hideTokenValueFromStringRepresentation() {
        IssuedAccessToken issuedAccessToken = accessTokenIssuer.issue(1L);

        assertThat(issuedAccessToken.toString())
                .doesNotContain(issuedAccessToken.value())
                .contains(issuedAccessToken.expiresAt().toString());
    }
}
