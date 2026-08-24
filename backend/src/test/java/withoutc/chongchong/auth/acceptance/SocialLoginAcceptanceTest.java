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
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private static final String SECOND_KAKAO_AUTHORIZATION_CODE = "second-kakao-authorization-code";
    private static final String SECOND_PROVIDER_USER_ID = "second-kakao-user-id";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

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

        assertProtectedApiAccessible(accessToken, user.getId());
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
    @DisplayName("웹 로그인부터 보호 API, 재발급과 로그아웃까지 전체 인증 생명주기를 수행한다")
    void completeWebAuthenticationLifecycle() {
        fakeSocialLoginClient.willSucceed(KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID,
                "총총이",
                null
        ));
        Response loginResponse = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);
        loginResponse.then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("accessToken", notNullValue())
                .body("$", not(hasKey("refreshToken")))
                .header(HttpHeaders.CACHE_CONTROL, containsString("no-store"))
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("HttpOnly"));

        String originalAccessToken = loginResponse.jsonPath().getString("accessToken");
        String originalRefreshToken = loginResponse.getCookie("refresh_token");
        User user = userRepository.findAll().getFirst();
        assertProtectedApiAccessible(originalAccessToken, user.getId());

        AuthSession originalSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        Long sessionId = originalSession.getId();
        HashedRefreshToken originalHash = originalSession.getRefreshTokenHash();

        Response refreshResponse = requestRefresh(originalRefreshToken);

        refreshResponse.then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("accessToken", notNullValue())
                .body("accessTokenExpiresAt", notNullValue())
                .body("$", not(hasKey("refreshToken")))
                .body("$", not(hasKey("refreshTokenExpiresAt")))
                .header(HttpHeaders.CACHE_CONTROL, containsString("no-store"))
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("Secure"))
                .header(HttpHeaders.SET_COOKIE, containsString("HttpOnly"))
                .header(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax"));

        String rotatedAccessToken = refreshResponse.jsonPath().getString("accessToken");
        String rotatedRefreshToken = refreshResponse.getCookie("refresh_token");
        HashedRefreshToken rotatedHash = refreshTokenHasher.hash(
                new RawRefreshToken(rotatedRefreshToken)
        );
        AuthSession rotatedSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);
        assertThat(rotatedAccessToken).isNotEqualTo(originalAccessToken);
        assertThat(rotatedSession.getId()).isEqualTo(sessionId);
        assertThat(rotatedSession.getRefreshTokenHash())
                .isEqualTo(rotatedHash)
                .isNotEqualTo(originalHash);
        assertThat(rotatedSession.getRefreshTokenHash().value())
                .isNotEqualTo(rotatedRefreshToken);

        Response reusedRefreshResponse = requestRefresh(originalRefreshToken);
        reusedRefreshResponse.then()
                .statusCode(401)
                .body("code", equalTo("INVALID_REFRESH_TOKEN"))
                .body("message", equalTo("유효하지 않은 Refresh Token입니다."))
                .header(HttpHeaders.SET_COOKIE, nullValue());
        assertThat(reusedRefreshResponse.asString()).doesNotContain(originalRefreshToken);

        assertProtectedApiAccessible(rotatedAccessToken, user.getId());

        Response logoutResponse = requestLogout(rotatedRefreshToken);
        logoutResponse.then()
                .statusCode(204)
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"))
                .header(HttpHeaders.SET_COOKIE, containsString("Path=/auth"));
        assertThat(logoutResponse.asString()).isEmpty();
        assertThat(authSessionRepository.findByUserId(user.getId())).isEmpty();

        Response refreshAfterLogoutResponse = requestRefresh(rotatedRefreshToken);
        refreshAfterLogoutResponse.then()
                .statusCode(401)
                .body("code", equalTo("INVALID_REFRESH_TOKEN"))
                .header(HttpHeaders.SET_COOKIE, nullValue());
        assertThat(refreshAfterLogoutResponse.asString())
                .doesNotContain(rotatedRefreshToken)
                .doesNotContain(rotatedAccessToken);
        assertThat(userRepository.count()).isOne();
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(authSessionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Refresh Cookie가 없으면 공통 401 오류를 반환한다")
    void rejectMissingRefreshCookie() {
        Response response = requestRefreshWithoutCookie();

        response.then()
                .statusCode(401)
                .body("code", equalTo("INVALID_REFRESH_TOKEN"))
                .body("message", equalTo("유효하지 않은 Refresh Token입니다."))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("DB에 없는 Refresh Cookie는 공통 401 오류로 반환하고 비밀값을 노출하지 않는다")
    void rejectUnknownRefreshCookieWithoutExposingSecret() {
        String unknownRefreshToken = "unknown-refresh-token";

        Response response = requestRefresh(unknownRefreshToken);

        response.then()
                .statusCode(401)
                .body("code", equalTo("INVALID_REFRESH_TOKEN"))
                .body("message", equalTo("유효하지 않은 Refresh Token입니다."))
                .header(HttpHeaders.SET_COOKIE, nullValue());
        assertThat(response.asString())
                .doesNotContain(unknownRefreshToken)
                .doesNotContain("uk_auth_sessions")
                .doesNotContain(refreshTokenHasher.hash(new RawRefreshToken(unknownRefreshToken)).value());
        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("만료된 Refresh Cookie는 Session을 변경하지 않고 공통 401 오류를 반환한다")
    void rejectExpiredRefreshCookieWithoutChangingSession() {
        fakeSocialLoginClient.willSucceed(KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID,
                "총총이",
                null
        ));
        Response loginResponse = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);
        String refreshToken = loginResponse.getCookie("refresh_token");
        User user = userRepository.findAll().getFirst();
        AuthSession authSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        HashedRefreshToken refreshTokenHash = authSession.getRefreshTokenHash();
        Instant expiredAt = Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.MICROS);
        authSession.replaceRefreshToken(refreshTokenHash, expiredAt);
        authSessionRepository.saveAndFlush(authSession);

        Response response = requestRefresh(refreshToken);

        response.then()
                .statusCode(401)
                .body("code", equalTo("INVALID_REFRESH_TOKEN"))
                .header(HttpHeaders.SET_COOKIE, nullValue());
        AuthSession unchangedSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(unchangedSession.getRefreshTokenHash()).isEqualTo(refreshTokenHash);
        assertThat(unchangedSession.getExpiresAt()).isEqualTo(expiredAt);
    }

    @Test
    @DisplayName("로그아웃하면 Session과 Refresh Cookie를 정리하고 기존 Access Token은 만료까지 유지한다")
    void logoutAndKeepIssuedAccessTokenUntilExpiration() {
        fakeSocialLoginClient.willSucceed(KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID,
                "총총이",
                null
        ));
        Response loginResponse = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);
        String accessToken = loginResponse.jsonPath().getString("accessToken");
        String refreshToken = loginResponse.getCookie("refresh_token");
        User user = userRepository.findAll().getFirst();

        Response logoutResponse = requestLogout(refreshToken);

        logoutResponse.then()
                .statusCode(204)
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"))
                .header(HttpHeaders.SET_COOKIE, containsString("Path=/auth"))
                .header(HttpHeaders.SET_COOKIE, containsString("Secure"))
                .header(HttpHeaders.SET_COOKIE, containsString("HttpOnly"))
                .header(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax"));
        assertThat(logoutResponse.asString()).isEmpty();
        assertThat(authSessionRepository.count()).isZero();

        requestRefresh(refreshToken)
                .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_REFRESH_TOKEN"))
                .header(HttpHeaders.SET_COOKIE, nullValue());

        given()
                .port(port)
                .auth().oauth2(accessToken)
                .when()
                .get("/test/auth-login/current-user")
                .then()
                .statusCode(200)
                .body(equalTo(user.getId().toString()));

        requestLogout(refreshToken)
                .then()
                .statusCode(204)
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"));
        assertThat(authSessionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Refresh Cookie가 없어도 로그아웃은 멱등하게 성공하고 Cookie를 만료시킨다")
    void logoutIdempotentlyWithoutRefreshCookie() {
        Response response = requestLogoutWithoutCookie();

        response.then()
                .statusCode(204)
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"))
                .header(HttpHeaders.SET_COOKIE, containsString("Path=/auth"));
        assertThat(response.asString()).isEmpty();
        assertDatabaseEmpty();
    }

    @Test
    @DisplayName("한 사용자의 로그아웃은 다른 사용자의 Session에 영향을 주지 않는다")
    void logoutWithoutChangingOtherUserSession() {
        fakeSocialLoginClient.willSucceed(KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID,
                "첫 번째 사용자",
                null
        ));
        fakeSocialLoginClient.willSucceed(SECOND_KAKAO_AUTHORIZATION_CODE, new SocialUserInfo(
                SocialProvider.KAKAO,
                SECOND_PROVIDER_USER_ID,
                "두 번째 사용자",
                null
        ));
        Response firstLoginResponse = requestLogin("KAKAO", KAKAO_AUTHORIZATION_CODE);
        Response secondLoginResponse = requestLogin("KAKAO", SECOND_KAKAO_AUTHORIZATION_CODE);
        String firstRefreshToken = firstLoginResponse.getCookie("refresh_token");
        String secondRefreshToken = secondLoginResponse.getCookie("refresh_token");
        SocialAccount firstSocialAccount = socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                PROVIDER_USER_ID
        ).orElseThrow();
        SocialAccount secondSocialAccount = socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                SECOND_PROVIDER_USER_ID
        ).orElseThrow();
        Long firstUserId = firstSocialAccount.getUser().getId();
        Long secondUserId = secondSocialAccount.getUser().getId();
        HashedRefreshToken secondSessionHash = authSessionRepository.findByUserId(secondUserId)
                .orElseThrow()
                .getRefreshTokenHash();

        requestLogout(firstRefreshToken)
                .then()
                .statusCode(204);

        assertThat(authSessionRepository.findByUserId(firstUserId)).isEmpty();
        assertThat(authSessionRepository.findByUserId(secondUserId))
                .hasValueSatisfying(session -> assertThat(session.getRefreshTokenHash())
                        .isEqualTo(secondSessionHash));
        assertThat(authSessionRepository.count()).isOne();

        requestRefresh(secondRefreshToken)
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
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
        return givenWithCsrf()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/auth/login");
    }

    private Response requestRefresh(String refreshToken) {
        return givenWithCsrf()
                .cookie("refresh_token", refreshToken)
                .when()
                .post("/auth/refresh");
    }

    private Response requestRefreshWithoutCookie() {
        return givenWithCsrf()
                .when()
                .post("/auth/refresh");
    }

    private Response requestLogout(String refreshToken) {
        return givenWithCsrf()
                .cookie("refresh_token", refreshToken)
                .when()
                .post("/auth/logout");
    }

    private Response requestLogoutWithoutCookie() {
        return givenWithCsrf()
                .when()
                .post("/auth/logout");
    }

    private RequestSpecification givenWithCsrf() {
        Response csrfResponse = given()
                .port(port)
                .when()
                .get("/auth/csrf");

        return given()
                .port(port)
                .cookie(CSRF_COOKIE_NAME, csrfResponse.getCookie(CSRF_COOKIE_NAME))
                .header(CSRF_HEADER_NAME, csrfResponse.jsonPath().getString("token"));
    }

    private void assertProtectedApiAccessible(String accessToken, Long expectedUserId) {
        given()
                .port(port)
                .auth().oauth2(accessToken)
                .when()
                .get("/test/auth-login/current-user")
                .then()
                .statusCode(200)
                .body(equalTo(expectedUserId.toString()));
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
