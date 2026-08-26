package withoutc.chongchong.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.support.TestJwtFactory;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebCsrfSecurityTest.BearerPostController.class)
@ActiveProfiles("test")
class WebCsrfSecurityTest {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";
    private static final String TRUSTED_ORIGIN = "https://test.chongchong.app";
    private static final String LOCAL_ORIGIN = "http://localhost:3005";
    private static final String UNTRUSTED_ORIGIN = "https://attacker.example";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Test
    @DisplayName("Access Token 없이 마스킹된 CSRF Token과 제한된 HttpOnly Cookie를 발급받는다")
    void issueCsrfTokenWithoutAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.headerName").value(CSRF_HEADER_NAME))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(CSRF_COOKIE_NAME + "=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("JSESSIONID"))))
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        assertThat(csrfCookie).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(csrfCookie.getValue()).isNotEqualTo(token);
        assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    @DisplayName("CSRF 정보가 없는 로그인 요청은 Provider 호출 전에 공통 403으로 거부한다")
    void rejectLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO",
                                  "authorizationCode": "must-not-be-exposed"
                                }
                                """))
                .andExpectAll(invalidCsrfTokenExpectations())
                .andExpect(content().string(not(containsString("must-not-be-exposed"))))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString(REFRESH_COOKIE_NAME + "="))));
    }

    @Test
    @DisplayName("Cookie와 일치하지 않는 CSRF Header는 세부 원문을 노출하지 않고 공통 403으로 거부한다")
    void rejectInvalidCsrfToken() throws Exception {
        CsrfCredentials csrf = issueCsrfCredentials();

        mockMvc.perform(post("/auth/refresh")
                        .cookie(csrf.cookie(), new Cookie(REFRESH_COOKIE_NAME, TEST_REFRESH_TOKEN))
                        .header(CSRF_HEADER_NAME, "tampered-csrf-token"))
                .andExpectAll(invalidCsrfTokenExpectations())
                .andExpect(content().string(not(containsString("tampered-csrf-token"))))
                .andExpect(content().string(not(containsString(csrf.token()))))
                .andExpect(content().string(not(containsString(TEST_REFRESH_TOKEN))))
                .andExpect(content().string(not(containsString("InvalidCsrfTokenException"))));
    }

    @Test
    @DisplayName("정상 CSRF 정보가 있으면 로그인 요청은 Security를 통과해 MVC 검증으로 전달된다")
    void allowLoginWithValidCsrfToken() throws Exception {
        CsrfCredentials csrf = issueCsrfCredentials();

        mockMvc.perform(post("/auth/login")
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER_NAME, csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("정상 CSRF 정보가 있으면 재발급 요청은 Refresh Token 검증까지 진행한다")
    void allowRefreshWithValidCsrfToken() throws Exception {
        CsrfCredentials csrf = issueCsrfCredentials();

        mockMvc.perform(post("/auth/refresh")
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER_NAME, csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("정상 CSRF 정보가 있으면 로그아웃 요청은 멱등하게 성공한다")
    void allowLogoutWithValidCsrfToken() throws Exception {
        CsrfCredentials csrf = issueCsrfCredentials();

        mockMvc.perform(post("/auth/logout")
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER_NAME, csrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_COOKIE_NAME + "=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    @DisplayName("CSRF 정보가 없는 로그아웃 요청은 Session과 Refresh Cookie를 변경하기 전에 거부한다")
    void rejectLogoutWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, TEST_REFRESH_TOKEN)))
                .andExpectAll(invalidCsrfTokenExpectations())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString(REFRESH_COOKIE_NAME + "="))))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("Max-Age=0"))))
                .andExpect(content().string(not(containsString(TEST_REFRESH_TOKEN))));
    }

    @Test
    @DisplayName("신뢰하는 Origin의 실제 요청에는 credential CORS Header를 부여한다")
    void allowTrustedOriginRequest() throws Exception {
        mockMvc.perform(get("/auth/csrf")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("신뢰하는 Origin의 사전 요청에는 필요한 Method와 Header를 허용한다")
    void allowTrustedOriginPreflight() throws Exception {
        mockMvc.perform(options("/auth/refresh")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CSRF_HEADER_NAME))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString(CSRF_HEADER_NAME)));
    }

    @Test
    @DisplayName("환경에 추가한 로컬 Origin의 사전 요청도 허용한다")
    void allowConfiguredLocalOriginPreflight() throws Exception {
        mockMvc.perform(options("/auth/refresh")
                        .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CSRF_HEADER_NAME))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("신뢰하지 않는 Origin의 Cookie 요청은 CORS에서 거부한다")
    void rejectUntrustedOriginRequest() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .header(HttpHeaders.ORIGIN, UNTRUSTED_ORIGIN)
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, TEST_REFRESH_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString(REFRESH_COOKIE_NAME + "="))))
                .andExpect(content().string(not(containsString(TEST_REFRESH_TOKEN))));
    }

    @Test
    @DisplayName("신뢰하지 않는 Origin의 사전 요청에는 CORS 허용 Header를 부여하지 않는다")
    void doNotAllowUntrustedOriginPreflight() throws Exception {
        mockMvc.perform(options("/auth/refresh")
                        .header(HttpHeaders.ORIGIN, UNTRUSTED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CSRF_HEADER_NAME))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Bearer Access Token을 사용하는 도메인 POST 요청에는 CSRF Token을 요구하지 않는다")
    void doNotRequireCsrfTokenForBearerPostRequest() throws Exception {
        String accessToken = testJwtFactory.accessToken(1L);

        mockMvc.perform(post("/test/csrf/bearer-only")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("1")));
    }

    @Test
    @DisplayName("인증되지 않은 도메인 POST 요청은 CSRF 403이 아니라 인증 401을 반환한다")
    void requireAccessTokenForBearerPostRequest() throws Exception {
        mockMvc.perform(post("/test/csrf/bearer-only"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private CsrfCredentials issueCsrfCredentials() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        assertThat(cookie).isNotNull();
        return new CsrfCredentials(cookie, token);
    }

    private ResultMatcher[] invalidCsrfTokenExpectations() {
        return new ResultMatcher[]{
                status().isForbidden(),
                content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                content().encoding(StandardCharsets.UTF_8),
                jsonPath("$.code").value("INVALID_CSRF_TOKEN"),
                jsonPath("$.message").value("유효하지 않은 CSRF Token입니다."),
                jsonPath("$.errors").doesNotExist()
        };
    }

    private record CsrfCredentials(Cookie cookie, String token) {
    }

    @RestController
    static class BearerPostController {

        @PostMapping("/test/csrf/bearer-only")
        Long bearerOnly(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
            return authenticatedUser.id();
        }
    }
}
