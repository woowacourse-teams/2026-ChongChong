package withoutc.chongchong.auth.support;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestJwtSupportTest.TestController.class)
@ActiveProfiles("test")
class TestJwtSupportTest {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @LocalServerPort
    private int port;

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Test
    @DisplayName("서로 다른 사용자 ID로 실제 인증 사용자를 준비한다")
    void authenticateDifferentUsers() {
        String firstAccessToken = testJwtFactory.accessToken(1L);
        String secondAccessToken = testJwtFactory.accessToken(2L);

        assertThat(firstAccessToken).isNotEqualTo(secondAccessToken);

        testAuthRequest.givenAuthenticatedUser(1L)
                .port(port)
                .when()
                .get("/test/auth-support/current-user")
                .then()
                .statusCode(200)
                .body(equalTo("1"));

        testAuthRequest.givenAuthenticatedUser(2L)
                .port(port)
                .when()
                .get("/test/auth-support/current-user")
                .then()
                .statusCode(200)
                .body(equalTo("2"));
    }

    @Test
    @DisplayName("Access Token 없이 보호 경로에 요청하면 401을 반환한다")
    void rejectRequestWithoutAccessToken() {
        given()
                .port(port)
                .when()
                .get("/test/auth-support/current-user")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("테스트 Factory가 만든 만료 Access Token을 실제 요청에서 거부한다")
    void rejectExpiredAccessToken() {
        testAuthRequest.givenAccessToken(testJwtFactory.expiredAccessToken(1L))
                .port(port)
                .when()
                .get("/test/auth-support/current-user")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("테스트 Factory가 만든 잘못된 issuer의 Access Token을 실제 요청에서 거부한다")
    void rejectAccessTokenWithInvalidIssuer() {
        testAuthRequest.givenAccessToken(testJwtFactory.invalidIssuerAccessToken(1L))
                .port(port)
                .when()
                .get("/test/auth-support/current-user")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("테스트 Factory가 만든 잘못된 audience의 Access Token을 실제 요청에서 거부한다")
    void rejectAccessTokenWithInvalidAudience() {
        testAuthRequest.givenAccessToken(testJwtFactory.invalidAudienceAccessToken(1L))
                .port(port)
                .when()
                .get("/test/auth-support/current-user")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("토큰 재발급 공개 경로는 Access Token 없이 Security를 통과한다")
    void allowRefreshPathWithoutAccessToken() {
        givenWithCsrf()
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("로그아웃 공개 경로는 Access Token 없이 Security를 통과한다")
    void allowLogoutPathWithoutAccessToken() {
        givenWithCsrf()
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(204)
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"));
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

    @RestController
    static class TestController {

        @GetMapping("/test/auth-support/current-user")
        Long currentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
            return authenticatedUser.id();
        }
    }
}
