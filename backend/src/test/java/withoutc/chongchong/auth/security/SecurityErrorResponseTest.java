package withoutc.chongchong.auth.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        SecurityErrorResponseTest.TestController.class,
        SecurityErrorResponseTest.DeniedSecurityConfig.class
})
@ActiveProfiles("test")
class SecurityErrorResponseTest {

    private static final String ISSUER = "chongchong-test";
    private static final String AUDIENCE = "chongchong-test-api";
    private static final byte[] SECRET =
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ANOTHER_SECRET =
            "abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("보호 경로에 Access Token이 없으면 공통 JSON 형식의 401을 반환한다")
    void respondUnauthorizedWhenAccessTokenIsMissing() throws Exception {
        expectAuthenticationRequired(mockMvc.perform(get("/test/security/protected")));
    }

    @Test
    @DisplayName("서명이 올바르지 않은 Access Token이면 공통 JSON 형식의 401을 반환한다")
    void respondUnauthorizedWhenAccessTokenHasInvalidSignature() throws Exception {
        String invalidToken = token(
                ANOTHER_SECRET,
                Instant.now(),
                Instant.now().plusSeconds(300)
        );

        expectAuthenticationRequired(mockMvc.perform(get("/test/security/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)))
                .andExpect(content().string(not(containsString(invalidToken))))
                .andExpect(content().string(not(containsString("signature"))));
    }

    @Test
    @DisplayName("만료된 Access Token이면 공통 JSON 형식의 401을 반환한다")
    void respondUnauthorizedWhenAccessTokenIsExpired() throws Exception {
        Instant expiresAt = Instant.now().minusSeconds(120);
        String expiredToken = token(SECRET, expiresAt.minusSeconds(300), expiresAt);

        expectAuthenticationRequired(mockMvc.perform(get("/test/security/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)))
                .andExpect(content().string(not(containsString(expiredToken))))
                .andExpect(content().string(not(containsString("expired"))));
    }

    @Test
    @DisplayName("인증된 사용자의 접근이 거부되면 공통 JSON 형식의 403을 반환한다")
    void respondForbiddenWhenAuthenticatedUserIsDenied() throws Exception {
        String accessToken = token(SECRET, Instant.now(), Instant.now().plusSeconds(300));

        mockMvc.perform(get("/test/security/forbidden")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("요청한 작업을 수행할 권한이 없습니다."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(content().string(not(containsString(accessToken))))
                .andExpect(content().string(not(containsString("AccessDeniedException"))));
    }

    @Test
    @DisplayName("공개 경로는 Access Token 없이 Security를 통과한다")
    void allowPublicPathWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    private ResultActions expectAuthenticationRequired(ResultActions resultActions) throws Exception {
        return resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    private String token(byte[] secret, Instant issuedAt, Instant expiresAt) {
        SecretKey secretKey = new SecretKeySpec(secret, "HmacSHA256");
        JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @RestController
    static class TestController {

        @GetMapping("/auth/login")
        String publicEndpoint() {
            return "public";
        }

        @GetMapping("/test/security/protected")
        String protectedEndpoint() {
            return "protected";
        }

        @GetMapping("/test/security/forbidden")
        String forbiddenEndpoint() {
            return "forbidden";
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DeniedSecurityConfig {

        @Bean
        @Order(0)
        SecurityFilterChain deniedSecurityFilterChain(
                HttpSecurity http,
                AuthenticatedUserJwtAuthenticationConverter jwtAuthenticationConverter,
                RestAuthenticationEntryPoint authenticationEntryPoint,
                RestAccessDeniedHandler accessDeniedHandler
        ) throws Exception {
            return http
                    .securityMatcher("/test/security/forbidden")
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler)
                    )
                    .oauth2ResourceServer(resourceServer -> resourceServer
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler)
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    )
                    .build();
        }
    }
}
