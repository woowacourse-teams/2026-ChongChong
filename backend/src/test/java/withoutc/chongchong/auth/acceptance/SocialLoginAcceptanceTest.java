package withoutc.chongchong.auth.acceptance;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.config.JwtProperties;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.entity.SocialAccount;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.support.FakeSocialLoginClient;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        SocialLoginAcceptanceTest.FakeSocialLoginConfig.class,
        SocialLoginAcceptanceTest.CurrentUserController.class
})
@ActiveProfiles("test")
class SocialLoginAcceptanceTest {

    private static final String KAKAO_AUTHORIZATION_CODE = "fake-kakao-authorization-code";
    private static final String PROVIDER_USER_ID = "kakao-user-id";

    @LocalServerPort
    private int port;

    @Autowired
    private FakeSocialLoginClient fakeSocialLoginClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanState() {
        fakeSocialLoginClient.clear();
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("신규 소셜 사용자가 로그인하고 발급 Access Token으로 보호 API에 접근한다")
    void loginNewSocialUserAndAuthenticateWithIssuedAccessToken() {
        fakeSocialLoginClient.willSucceed(KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID,
                "총총이",
                "https://example.com/profile.png"
        ));

        Response response = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);

        response.then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("accessToken", notNullValue())
                .body("accessTokenExpiresAt", notNullValue())
                .body("$", not(hasKey("refreshToken")))
                .body("$", not(hasKey("refreshTokenExpiresAt")))
                .body("$", not(hasKey("authorizationCode")))
                .body("$", not(hasKey("userId")))
                .body("$", not(hasKey("sessionId")))
                .body("$", not(hasKey("refreshTokenHash")))
                .header(HttpHeaders.CACHE_CONTROL, containsString("no-store"))
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("Path=/auth"))
                .header(HttpHeaders.SET_COOKIE, containsString("Secure"))
                .header(HttpHeaders.SET_COOKIE, containsString("HttpOnly"))
                .header(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax"));

        String accessToken = response.jsonPath().getString("accessToken");
        String refreshToken = response.getCookie("refresh_token");
        Instant accessTokenExpiresAt = Instant.parse(
                response.jsonPath().getString("accessTokenExpiresAt")
        );

        User user = userRepository.findAll().getFirst();
        SocialAccount socialAccount = socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID
        ).orElseThrow();
        AuthSession authSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        HashedRefreshToken expectedRefreshTokenHash = refreshTokenHasher.hash(
                new RawRefreshToken(refreshToken)
        );
        Jwt jwt = jwtDecoder.decode(accessToken);

        assertThat(userRepository.count()).isOne();
        assertThat(user.getName()).isEqualTo("총총이");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(socialAccount.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(socialAccount.getProviderUserId()).isEqualTo(PROVIDER_USER_ID);
        assertThat(socialAccount.getUser().getId()).isEqualTo(user.getId());
        assertThat(authSessionRepository.count()).isOne();
        assertThat(authSession.getUser().getId()).isEqualTo(user.getId());
        assertThat(authSession.getRefreshTokenHash()).isEqualTo(expectedRefreshTokenHash);
        assertThat(authSession.getRefreshTokenHash().value()).isNotEqualTo(refreshToken);
        assertThat(authSession.getExpiresAt()).isAfter(accessTokenExpiresAt);
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(jwtProperties.issuer());
        assertThat(jwt.getAudience()).containsExactly(jwtProperties.audience());
        assertThat(jwt.getExpiresAt()).isEqualTo(accessTokenExpiresAt);

        given()
                .port(port)
                .auth().oauth2(accessToken)
                .when()
                .get("/test/auth-login/current-user")
                .then()
                .statusCode(200)
                .body(equalTo(user.getId().toString()));
    }

    @Test
    @DisplayName("같은 소셜 사용자가 다시 로그인하면 사용자와 계정을 재사용하고 인증 세션을 교체한다")
    void reuseSocialUserAndReplaceAuthSessionOnRelogin() {
        fakeSocialLoginClient.willSucceed(KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID,
                "처음 이름",
                "https://example.com/first-profile.png"
        ));
        Response firstResponse = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);
        firstResponse.then().statusCode(200);
        User firstUser = userRepository.findAll().getFirst();
        AuthSession firstSession = authSessionRepository.findByUserId(firstUser.getId()).orElseThrow();
        Long firstSessionId = firstSession.getId();
        HashedRefreshToken firstRefreshTokenHash = firstSession.getRefreshTokenHash();
        String firstRefreshToken = firstResponse.getCookie("refresh_token");

        fakeSocialLoginClient.willSucceed(KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID,
                "바뀐 이름",
                "https://example.com/changed-profile.png"
        ));
        Response secondResponse = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);

        secondResponse.then().statusCode(200);
        User reusedUser = userRepository.findAll().getFirst();
        AuthSession replacedSession = authSessionRepository.findByUserId(reusedUser.getId()).orElseThrow();
        String secondAccessToken = secondResponse.jsonPath().getString("accessToken");
        String secondRefreshToken = secondResponse.getCookie("refresh_token");

        assertThat(userRepository.count()).isOne();
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(authSessionRepository.count()).isOne();
        assertThat(reusedUser.getId()).isEqualTo(firstUser.getId());
        assertThat(reusedUser.getName()).isEqualTo("처음 이름");
        assertThat(reusedUser.getProfileImageUrl()).isEqualTo("https://example.com/first-profile.png");
        assertThat(replacedSession.getId()).isEqualTo(firstSessionId);
        assertThat(replacedSession.getRefreshTokenHash()).isNotEqualTo(firstRefreshTokenHash);
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(jwtDecoder.decode(secondAccessToken).getSubject())
                .isEqualTo(firstUser.getId().toString());
    }

    @Test
    @DisplayName("Provider 인증에 실패하면 Token을 반환하거나 로그인 데이터를 저장하지 않는다")
    void rejectProviderAuthenticationFailure() {
        fakeSocialLoginClient.willFail(KAKAO_AUTHORIZATION_CODE);

        Response response = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);

        response.then()
                .statusCode(401)
                .body("code", equalTo("SOCIAL_AUTHENTICATION_FAILED"))
                .body("message", equalTo("소셜 로그인 인증에 실패했습니다."))
                .body("errors", nullValue())
                .body("$", not(hasKey("accessToken")))
                .body("$", not(hasKey("refreshToken")))
                .body("$", not(hasKey("accessTokenExpiresAt")))
                .body("$", not(hasKey("refreshTokenExpiresAt")))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        assertThat(response.asString()).doesNotContain(KAKAO_AUTHORIZATION_CODE);
        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("등록되지 않은 Provider는 공통 Auth 오류로 거부한다")
    void rejectUnsupportedProvider() {
        Response response = requestLogin("GOOGLE", "fake-google-authorization-code");

        response.then()
                .statusCode(400)
                .body("code", equalTo("UNSUPPORTED_SOCIAL_PROVIDER"))
                .body("message", equalTo("지원하지 않는 소셜 로그인 제공자입니다."))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("provider가 누락되면 공통 입력 오류로 거부한다")
    void rejectMissingProvider() {
        Response response = requestLoginBody("""
                {
                  "authorizationCode": "fake-kakao-authorization-code"
                }
                """);

        response.then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors.field", hasItem("provider"))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("Kakao 인가 코드가 공백이면 공통 입력 오류로 거부한다")
    void rejectBlankAuthorizationCode() {
        Response response = requestLogin("KAKAO", " ");

        response.then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors.field", hasItem("authorizationCode"))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("알 수 없는 Provider 문자열은 공통 잘못된 요청 오류로 거부한다")
    void rejectUnknownProvider() {
        Response response = requestLogin("UNKNOWN", KAKAO_AUTHORIZATION_CODE);

        response.then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"))
                .body("message", equalTo("요청 형식이 잘못되었습니다."))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        assertThat(response.asString()).doesNotContain(KAKAO_AUTHORIZATION_CODE);
        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("파싱할 수 없는 JSON은 공통 잘못된 요청 오류로 거부한다")
    void rejectMalformedJson() {
        Response response = requestLoginBody("""
                {
                  "provider":
                }
                """);

        response.then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"))
                .body("message", equalTo("요청 형식이 잘못되었습니다."))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        assertDatabaseEmpty();
    }

    private Response requestLogin(
            String provider,
            String authorizationCode
    ) {
        return requestLoginBody("""
                {
                  "provider": "%s",
                  "authorizationCode": "%s"
                }
                """.formatted(provider, authorizationCode));
    }

    private Response requestLoginBody(String body) {
        return given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/auth/login");
    }

    private void assertDatabaseEmpty() {
        assertThat(userRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
        assertThat(authSessionRepository.count()).isZero();
    }

    @RestController
    static class CurrentUserController {

        @GetMapping("/test/auth-login/current-user")
        Long currentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
            return authenticatedUser.id();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeSocialLoginConfig {

        @Bean
        FakeSocialLoginClient fakeSocialLoginClient() {
            return new FakeSocialLoginClient(SocialProvider.KAKAO);
        }
    }
}
