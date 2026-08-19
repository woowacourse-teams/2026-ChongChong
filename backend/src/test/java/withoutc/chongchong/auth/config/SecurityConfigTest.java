package withoutc.chongchong.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class SecurityConfigTest {

    private static final String ISSUER = "chongchong-test";
    private static final String AUDIENCE = "chongchong-test-api";
    private static final byte[] SECRET = "01234567890123456789012345678901".getBytes();
    private static final String ENCODED_SECRET = Base64.getEncoder().encodeToString(SECRET);

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtDecoder jwtDecoder = securityConfig.jwtDecoder(properties(ENCODED_SECRET));

    @Test
    @DisplayName("서명과 표준 Claim이 유효한 Access Token을 검증한다")
    void decodeValidAccessToken() {
        String token = token(SECRET, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300));

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("1");
    }

    @Test
    @DisplayName("다른 키로 서명한 Access Token을 거부한다")
    void rejectAccessTokenWithInvalidSignature() {
        byte[] anotherSecret = "abcdefghijklmnopqrstuvwxyz123456".getBytes();
        String token = token(anotherSecret, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("만료된 Access Token을 거부한다")
    void rejectExpiredAccessToken() {
        String token = token(SECRET, ISSUER, List.of(AUDIENCE), Instant.now().minusSeconds(120));

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("발급자가 다른 Access Token을 거부한다")
    void rejectAccessTokenWithInvalidIssuer() {
        String token = token(SECRET, "other-issuer", List.of(AUDIENCE), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("대상 API가 다른 Access Token을 거부한다")
    void rejectAccessTokenWithInvalidAudience() {
        String token = token(SECRET, ISSUER, List.of("other-api"), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Base64 형식이 아닌 JWT 서명 키를 거부한다")
    void rejectInvalidBase64Secret() {
        JwtProperties properties = properties("not-base64-secret!");

        assertThatThrownBy(() -> securityConfig.jwtDecoder(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT 서명 키는 Base64 형식이어야 합니다.");
    }

    @Test
    @DisplayName("256비트보다 짧은 JWT 서명 키를 거부한다")
    void rejectShortSecret() {
        String shortSecret = Base64.getEncoder().encodeToString("short-secret".getBytes());
        JwtProperties properties = properties(shortSecret);

        assertThatThrownBy(() -> securityConfig.jwtDecoder(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT 서명 키는 256비트 이상이어야 합니다.");
    }

    private JwtProperties properties(String encodedSecret) {
        return new JwtProperties(ISSUER, AUDIENCE, encodedSecret);
    }

    private String token(
            byte[] secret,
            String issuer,
            List<String> audience,
            Instant expiresAt
    ) {
        SecretKey secretKey = new SecretKeySpec(secret, "HmacSHA256");
        JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        Instant issuedAt = expiresAt.isAfter(Instant.now())
                ? Instant.now()
                : expiresAt.minusSeconds(300);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(audience)
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
