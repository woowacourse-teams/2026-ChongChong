package withoutc.chongchong.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.service.AuthTokenService;
import withoutc.chongchong.auth.service.SocialLoginFacade;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.token.IssuedAccessToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerTest.FixedClockConfig.class)
class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final String KAKAO_AUTHORIZATION_CODE = "test-kakao-authorization-code";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";
    private static final String CURRENT_REFRESH_TOKEN = "current-refresh-token";
    private static final Instant ACCESS_TOKEN_EXPIRES_AT = Instant.parse("2026-08-21T01:00:00Z");
    private static final Instant REFRESH_TOKEN_EXPIRES_AT = Instant.parse("2026-09-20T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocialLoginFacade socialLoginFacade;

    @MockitoBean
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("Access Token 없이 로그인하고 Access Token JSON과 Refresh Token Cookie를 받는다")
    void loginWithoutAccessToken() throws Exception {
        when(socialLoginFacade.login(any())).thenReturn(createIssuedTokenPair());

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO",
                                  "authorizationCode": "%s"
                                }
                                """.formatted(KAKAO_AUTHORIZATION_CODE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.accessTokenExpiresAt").value("2026-08-21T01:00:00Z"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.refreshTokenExpiresAt").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.authorizationCode").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(
                        "refresh_token=" + REFRESH_TOKEN
                )))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=2592000")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))))
                .andExpect(content().string(not(containsString(KAKAO_AUTHORIZATION_CODE))));

        verify(socialLoginFacade).login(new SocialLoginCommand(
                SocialProvider.KAKAO,
                KAKAO_AUTHORIZATION_CODE
        ));
    }

    @Test
    @DisplayName("provider가 누락되면 공통 입력 오류를 반환한다")
    void rejectMissingProvider() throws Exception {
        expectInvalidInput(mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorizationCode": "%s"
                                }
                                """.formatted(KAKAO_AUTHORIZATION_CODE))), "provider")
                .andExpect(content().string(not(containsString(KAKAO_AUTHORIZATION_CODE))));

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("authorizationCode가 누락되면 공통 입력 오류를 반환한다")
    void rejectMissingAuthorizationCode() throws Exception {
        expectInvalidInput(mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO"
                                }
                                """)), "authorizationCode");

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("authorizationCode가 공백이면 공통 입력 오류를 반환한다")
    void rejectBlankAuthorizationCode() throws Exception {
        expectInvalidInput(mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO",
                                  "authorizationCode": " "
                                }
                                """)), "authorizationCode");

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("알 수 없는 provider 문자열이면 공통 잘못된 요청 오류를 반환한다")
    void rejectUnknownProvider() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "UNKNOWN",
                                  "authorizationCode": "%s"
                                }
                                """.formatted(KAKAO_AUTHORIZATION_CODE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 형식이 잘못되었습니다."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(content().string(not(containsString(KAKAO_AUTHORIZATION_CODE))));

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("등록되지 않은 provider이면 공통 Auth 오류를 반환한다")
    void rejectUnsupportedProvider() throws Exception {
        when(socialLoginFacade.login(any()))
                .thenThrow(new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));

        expectAuthError(
                mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "authorizationCode": "%s"
                                }
                                """.formatted(KAKAO_AUTHORIZATION_CODE))),
                400,
                "UNSUPPORTED_SOCIAL_PROVIDER",
                "지원하지 않는 소셜 로그인 제공자입니다."
        );
    }

    @Test
    @DisplayName("Provider 인증에 실패하면 Token을 포함하지 않은 공통 Auth 오류를 반환한다")
    void rejectInvalidProviderCredential() throws Exception {
        when(socialLoginFacade.login(any()))
                .thenThrow(new AuthException(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED));

        expectAuthError(
                mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO",
                                  "authorizationCode": "%s"
                                }
                                """.formatted(KAKAO_AUTHORIZATION_CODE))),
                401,
                "SOCIAL_AUTHENTICATION_FAILED",
                "소셜 로그인 인증에 실패했습니다."
        );
    }

    @Test
    @DisplayName("Refresh Cookie로 새 Access Token JSON과 Refresh Cookie를 받는다")
    void refreshTokenPairWithCookie() throws Exception {
        when(authTokenService.rotate(new RawRefreshToken(CURRENT_REFRESH_TOKEN)))
                .thenReturn(createIssuedTokenPair());

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", CURRENT_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.accessTokenExpiresAt").value("2026-08-21T01:00:00Z"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.refreshTokenExpiresAt").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(
                        "refresh_token=" + REFRESH_TOKEN
                )))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))))
                .andExpect(content().string(not(containsString(CURRENT_REFRESH_TOKEN))));

        verify(authTokenService).rotate(new RawRefreshToken(CURRENT_REFRESH_TOKEN));
        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("Refresh Cookie가 없으면 공통 401 오류를 반환한다")
    void rejectMissingRefreshCookie() throws Exception {
        expectInvalidRefreshToken(mockMvc.perform(post("/auth/refresh").with(csrf())));

        verifyNoInteractions(authTokenService, socialLoginFacade);
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Cookie는 Token을 노출하지 않는 공통 401 오류를 반환한다")
    void rejectInvalidRefreshCookie() throws Exception {
        when(authTokenService.rotate(any()))
                .thenThrow(new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        expectInvalidRefreshToken(mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", CURRENT_REFRESH_TOKEN))))
                .andExpect(content().string(not(containsString(CURRENT_REFRESH_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        verify(authTokenService).rotate(new RawRefreshToken(CURRENT_REFRESH_TOKEN));
        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("Refresh Cookie로 로그아웃하고 같은 범위의 Cookie를 만료시킨다")
    void logoutWithRefreshCookie() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", CURRENT_REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));

        verify(authTokenService).logout(new RawRefreshToken(CURRENT_REFRESH_TOKEN));
        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("Refresh Cookie가 없어도 로그아웃은 멱등하게 성공하고 Cookie를 만료시킨다")
    void logoutIdempotentlyWithoutRefreshCookie() throws Exception {
        mockMvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/auth")));

        verifyNoInteractions(authTokenService, socialLoginFacade);
    }

    private ResultActions expectInvalidRefreshToken(ResultActions resultActions) throws Exception {
        return resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 Refresh Token입니다."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    private ResultActions expectInvalidInput(
            ResultActions resultActions,
            String invalidField
    ) throws Exception {
        return resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors[0].field").value(invalidField))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    private void expectAuthError(
            ResultActions resultActions,
            int status,
            String code,
            String message
    ) throws Exception {
        resultActions
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(content().string(not(containsString(KAKAO_AUTHORIZATION_CODE))))
                .andExpect(content().string(not(containsString(ACCESS_TOKEN))))
                .andExpect(content().string(not(containsString(REFRESH_TOKEN))));

        verify(socialLoginFacade).login(any());
    }

    private IssuedTokenPair createIssuedTokenPair() {
        return new IssuedTokenPair(
                new IssuedAccessToken(ACCESS_TOKEN, ACCESS_TOKEN_EXPIRES_AT),
                new RawRefreshToken(REFRESH_TOKEN),
                REFRESH_TOKEN_EXPIRES_AT
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
