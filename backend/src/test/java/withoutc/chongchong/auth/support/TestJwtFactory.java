package withoutc.chongchong.auth.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import withoutc.chongchong.auth.config.JwtProperties;

@Component
@Profile("test")
public final class TestJwtFactory {

    private static final long ACCESS_TOKEN_VALIDITY_SECONDS = 300;
    private static final long EXPIRED_SECONDS = 120;
    private static final String INVALID_ISSUER = "invalid-test-issuer";
    private static final String INVALID_AUDIENCE = "invalid-test-audience";
    private static final byte[] INVALID_SIGNATURE_SECRET =
            "abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8);

    private final JwtProperties properties;
    private final byte[] secret;

    public TestJwtFactory(JwtProperties properties) {
        this.properties = properties;
        this.secret = Base64.getDecoder().decode(properties.secretBase64());
    }

    public String accessToken(Long userId) {
        return token(
                subject(userId),
                properties.issuer(),
                List.of(properties.audience()),
                secret,
                Instant.now(),
                Instant.now().plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS)
        );
    }

    public String expiredAccessToken(Long userId) {
        Instant expiresAt = Instant.now().minusSeconds(EXPIRED_SECONDS);

        return token(
                subject(userId),
                properties.issuer(),
                List.of(properties.audience()),
                secret,
                expiresAt.minusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS),
                expiresAt
        );
    }

    public String invalidIssuerAccessToken(Long userId) {
        return token(
                subject(userId),
                INVALID_ISSUER,
                List.of(properties.audience()),
                secret,
                Instant.now(),
                Instant.now().plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS)
        );
    }

    public String invalidAudienceAccessToken(Long userId) {
        return token(
                subject(userId),
                properties.issuer(),
                List.of(INVALID_AUDIENCE),
                secret,
                Instant.now(),
                Instant.now().plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS)
        );
    }

    public String invalidSignatureAccessToken(Long userId) {
        return token(
                subject(userId),
                properties.issuer(),
                List.of(properties.audience()),
                INVALID_SIGNATURE_SECRET,
                Instant.now(),
                Instant.now().plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS)
        );
    }

    public String accessTokenWithSubject(String subject) {
        return token(
                subject,
                properties.issuer(),
                List.of(properties.audience()),
                secret,
                Instant.now(),
                Instant.now().plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS)
        );
    }

    private String subject(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("테스트 사용자 ID는 양수여야 합니다.");
        }

        return userId.toString();
    }

    private String token(
            String subject,
            String issuer,
            List<String> audience,
            byte[] signingSecret,
            Instant issuedAt,
            Instant expiresAt
    ) {
        SecretKey secretKey = new SecretKeySpec(signingSecret, "HmacSHA256");
        JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(audience)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
