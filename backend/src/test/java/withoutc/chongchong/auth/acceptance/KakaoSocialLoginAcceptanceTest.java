package withoutc.chongchong.auth.acceptance;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.kakao.KakaoSocialLoginClient;
import withoutc.chongchong.auth.social.kakao.KakaoTokenClient;
import withoutc.chongchong.auth.social.kakao.KakaoUserInfoClient;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        KakaoSocialLoginAcceptanceTest.ActualKakaoAdapterTestConfig.class,
        KakaoSocialLoginAcceptanceTest.CurrentUserController.class
})
@ActiveProfiles("test")
class KakaoSocialLoginAcceptanceTest {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String FIRST_AUTHORIZATION_CODE = "first-sensitive-authorization-code";
    private static final String SECOND_AUTHORIZATION_CODE = "second-sensitive-authorization-code";
    private static final String FIRST_KAKAO_ACCESS_TOKEN = "first-sensitive-kakao-access-token";
    private static final String SECOND_KAKAO_ACCESS_TOKEN = "second-sensitive-kakao-access-token";
    private static final String PROVIDER_ERROR = "sensitive-kakao-provider-error";
    private static final KakaoStubServer KAKAO_STUB = new KakaoStubServer();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @DynamicPropertySource
    static void kakaoProperties(DynamicPropertyRegistry registry) {
        KAKAO_STUB.start();
        registry.add("auth.social.kakao.token-uri", KAKAO_STUB::tokenUri);
        registry.add("auth.social.kakao.user-info-uri", KAKAO_STUB::userInfoUri);
    }

    @AfterAll
    static void stopKakaoStub() {
        KAKAO_STUB.stop();
    }

    @BeforeEach
    @AfterEach
    void cleanState() {
        KAKAO_STUB.reset();
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("Kakao Code를 검증해 신규 사용자를 로그인시키고 발급 Access Token으로 보호 API에 접근한다")
    void loginNewKakaoUserAndAccessProtectedApi() {
        KAKAO_STUB.succeed(
                FIRST_KAKAO_ACCESS_TOKEN,
                123456789L,
                "총총이",
                "https://example.com/profile.png"
        );

        Response response = requestLogin(FIRST_AUTHORIZATION_CODE);

        response.then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("accessToken", notNullValue())
                .body("accessTokenExpiresAt", notNullValue())
                .body("$", not(hasKey("refreshToken")))
                .header(HttpHeaders.CACHE_CONTROL, containsString("no-store"))
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("HttpOnly"));

        String chongchongAccessToken = response.jsonPath().getString("accessToken");
        User user = userRepository.findAll().getFirst();

        assertThat(user.getName()).isEqualTo("총총이");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.KAKAO,
                "123456789"
        )).isPresent();
        assertThat(authSessionRepository.findByUserId(user.getId())).isPresent();
        assertThat(userRepository.count()).isOne();
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(authSessionRepository.count()).isOne();
        assertKakaoRequests(FIRST_AUTHORIZATION_CODE, FIRST_KAKAO_ACCESS_TOKEN);
        assertSensitiveValuesAbsent(response, FIRST_AUTHORIZATION_CODE, FIRST_KAKAO_ACCESS_TOKEN);
        assertProtectedApiAccessible(chongchongAccessToken, user.getId());
    }

    @Test
    @DisplayName("같은 Kakao 회원이 다시 로그인하면 사용자와 계정을 재사용하고 Session을 교체한다")
    void reuseKakaoUserAndReplaceSessionOnRelogin() {
        KAKAO_STUB.succeed(FIRST_KAKAO_ACCESS_TOKEN, 123456789L, "처음 이름", null);
        Response firstResponse = requestLogin(FIRST_AUTHORIZATION_CODE);
        firstResponse.then().statusCode(200);
        User firstUser = userRepository.findAll().getFirst();
        AuthSession firstSession = authSessionRepository.findByUserId(firstUser.getId()).orElseThrow();
        Long sessionId = firstSession.getId();
        String firstRefreshToken = firstResponse.getCookie("refresh_token");

        KAKAO_STUB.succeed(SECOND_KAKAO_ACCESS_TOKEN, 123456789L, "바뀐 Kakao 이름", null);
        Response secondResponse = requestLogin(SECOND_AUTHORIZATION_CODE);

        secondResponse.then().statusCode(200);
        User reusedUser = userRepository.findAll().getFirst();
        AuthSession replacedSession = authSessionRepository.findByUserId(reusedUser.getId()).orElseThrow();
        String secondRefreshToken = secondResponse.getCookie("refresh_token");

        assertThat(userRepository.count()).isOne();
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(authSessionRepository.count()).isOne();
        assertThat(reusedUser.getId()).isEqualTo(firstUser.getId());
        assertThat(reusedUser.getName()).isEqualTo("처음 이름");
        assertThat(replacedSession.getId()).isEqualTo(sessionId);
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(KAKAO_STUB.tokenRequests()).hasSize(2);
        assertThat(KAKAO_STUB.userInfoRequests()).hasSize(2);
        assertSensitiveValuesAbsent(
                secondResponse,
                SECOND_AUTHORIZATION_CODE,
                SECOND_KAKAO_ACCESS_TOKEN
        );
    }

    @Test
    @DisplayName("Kakao Token 교환이 실패하면 로그인 데이터를 만들지 않는다")
    void keepDatabaseEmptyWhenTokenExchangeFails() {
        KAKAO_STUB.failToken(400, PROVIDER_ERROR);

        Response response = requestLogin(FIRST_AUTHORIZATION_CODE);

        assertSocialAuthenticationFailed(response);
        assertThat(KAKAO_STUB.tokenRequests()).hasSize(1);
        assertThat(KAKAO_STUB.userInfoRequests()).isEmpty();
        assertDatabaseEmpty();
        assertSensitiveValuesAbsent(response, FIRST_AUTHORIZATION_CODE, PROVIDER_ERROR);
    }

    @Test
    @DisplayName("Kakao Token 교환 뒤 사용자 정보 조회가 실패해도 로그인 데이터를 만들지 않는다")
    void keepDatabaseEmptyWhenUserInfoFails() {
        KAKAO_STUB.failUserInfo(FIRST_KAKAO_ACCESS_TOKEN, 500, PROVIDER_ERROR);

        Response response = requestLogin(FIRST_AUTHORIZATION_CODE);

        assertSocialAuthenticationFailed(response);
        assertKakaoRequests(FIRST_AUTHORIZATION_CODE, FIRST_KAKAO_ACCESS_TOKEN);
        assertDatabaseEmpty();
        assertSensitiveValuesAbsent(
                response,
                FIRST_AUTHORIZATION_CODE,
                FIRST_KAKAO_ACCESS_TOKEN,
                PROVIDER_ERROR
        );
    }

    private Response requestLogin(String authorizationCode) {
        return givenWithCsrf()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "provider": "KAKAO",
                          "authorizationCode": "%s"
                        }
                        """.formatted(authorizationCode))
                .when()
                .post("/auth/login");
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

    private void assertSocialAuthenticationFailed(Response response) {
        response.then()
                .statusCode(401)
                .body("code", equalTo("SOCIAL_AUTHENTICATION_FAILED"))
                .body("message", equalTo("소셜 로그인 인증에 실패했습니다."))
                .header(HttpHeaders.SET_COOKIE, nullValue());
    }

    private void assertKakaoRequests(
            String authorizationCode,
            String kakaoAccessToken
    ) {
        assertThat(KAKAO_STUB.tokenRequests()).last().satisfies(request -> {
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.contentType()).startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            assertThat(parseForm(request.body()))
                    .containsEntry("grant_type", "authorization_code")
                    .containsEntry("code", authorizationCode)
                    .containsEntry("redirect_uri", "http://localhost:3005/auth/kakao/callback")
                    .containsKeys("client_id", "client_secret");
        });
        assertThat(KAKAO_STUB.userInfoRequests()).last().satisfies(request -> {
            assertThat(request.method()).isEqualTo("GET");
            assertThat(request.query()).isEqualTo("secure_resource=true");
            assertThat(request.authorization()).isEqualTo("Bearer " + kakaoAccessToken);
        });
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        Arrays.stream(body.split("&"))
                .map(field -> field.split("=", 2))
                .forEach(field -> values.put(decode(field[0]), decode(field[1])));
        return values;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void assertSensitiveValuesAbsent(Response response, String... sensitiveValues) {
        assertThat(response.asString()).doesNotContain(sensitiveValues);
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        if (setCookie != null) {
            assertThat(setCookie).doesNotContain(sensitiveValues);
        }
    }

    private void assertProtectedApiAccessible(String accessToken, Long expectedUserId) {
        given()
                .port(port)
                .auth().oauth2(accessToken)
                .when()
                .get("/test/kakao-auth/current-user")
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

        @GetMapping("/test/kakao-auth/current-user")
        Long currentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
            return authenticatedUser.id();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ActualKakaoAdapterTestConfig {

        @Bean
        KakaoSocialLoginClient kakaoSocialLoginClient(
                KakaoTokenClient kakaoTokenClient,
                KakaoUserInfoClient kakaoUserInfoClient
        ) {
            return new KakaoSocialLoginClient(kakaoTokenClient, kakaoUserInfoClient);
        }
    }

    private static final class KakaoStubServer {

        private final AtomicReference<StubResponse> tokenResponse = new AtomicReference<>();
        private final AtomicReference<StubResponse> userInfoResponse = new AtomicReference<>();
        private final CopyOnWriteArrayList<TokenRequest> tokenRequests = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<UserInfoRequest> userInfoRequests = new CopyOnWriteArrayList<>();

        private HttpServer server;

        synchronized void start() {
            if (server != null) {
                return;
            }
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                server.createContext("/oauth/token", this::handleToken);
                server.createContext("/v2/user/me", this::handleUserInfo);
                server.start();
                reset();
            } catch (IOException exception) {
                throw new IllegalStateException("Kakao Stub 서버를 시작할 수 없습니다.", exception);
            }
        }

        synchronized void stop() {
            if (server == null) {
                return;
            }
            server.stop(0);
            server = null;
        }

        void reset() {
            tokenRequests.clear();
            userInfoRequests.clear();
            tokenResponse.set(jsonResponse(500, "{\"error\":\"stub-not-configured\"}"));
            userInfoResponse.set(jsonResponse(500, "{\"msg\":\"stub-not-configured\"}"));
        }

        void succeed(
                String kakaoAccessToken,
                long providerUserId,
                String nickname,
                String profileImageUrl
        ) {
            tokenResponse.set(jsonResponse(200, """
                    {
                      "token_type": "bearer",
                      "access_token": "%s",
                      "expires_in": 43199,
                      "refresh_token": "unused-kakao-refresh-token"
                    }
                    """.formatted(kakaoAccessToken)));
            userInfoResponse.set(jsonResponse(200, userInfoBody(
                    providerUserId,
                    nickname,
                    profileImageUrl
            )));
        }

        void failToken(int status, String providerError) {
            tokenResponse.set(jsonResponse(status, """
                    {"error":"invalid_grant","error_description":"%s"}
                    """.formatted(providerError)));
        }

        void failUserInfo(
                String kakaoAccessToken,
                int status,
                String providerError
        ) {
            tokenResponse.set(jsonResponse(200, """
                    {"access_token":"%s"}
                    """.formatted(kakaoAccessToken)));
            userInfoResponse.set(jsonResponse(status, """
                    {"msg":"%s","code":-1}
                    """.formatted(providerError)));
        }

        String tokenUri() {
            return baseUri() + "/oauth/token";
        }

        String userInfoUri() {
            return baseUri() + "/v2/user/me";
        }

        CopyOnWriteArrayList<TokenRequest> tokenRequests() {
            return tokenRequests;
        }

        CopyOnWriteArrayList<UserInfoRequest> userInfoRequests() {
            return userInfoRequests;
        }

        private String baseUri() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void handleToken(HttpExchange exchange) throws IOException {
            tokenRequests.add(new TokenRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
            ));
            respond(exchange, tokenResponse.get());
        }

        private void handleUserInfo(HttpExchange exchange) throws IOException {
            userInfoRequests.add(new UserInfoRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getRawQuery(),
                    exchange.getRequestHeaders().getFirst("Authorization")
            ));
            respond(exchange, userInfoResponse.get());
        }

        private void respond(HttpExchange exchange, StubResponse response) throws IOException {
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(response.status(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }

        private StubResponse jsonResponse(int status, String body) {
            return new StubResponse(status, body);
        }

        private String userInfoBody(
                long providerUserId,
                String nickname,
                String profileImageUrl
        ) {
            String profileImageField = profileImageUrl == null
                    ? ""
                    : ",\"profile_image_url\":\"" + profileImageUrl + "\"";
            return """
                    {
                      "id": %d,
                      "kakao_account": {
                        "profile": {
                          "nickname": "%s"%s
                        }
                      }
                    }
                    """.formatted(providerUserId, nickname, profileImageField);
        }
    }

    private record StubResponse(int status, String body) {
    }

    private record TokenRequest(String method, String contentType, String body) {
    }

    private record UserInfoRequest(String method, String query, String authorization) {
    }
}
