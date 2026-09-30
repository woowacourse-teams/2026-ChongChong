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
import withoutc.chongchong.auth.service.SocialLoginFacade;
import withoutc.chongchong.auth.service.SocialLoginResult;
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
    private static final Long USER_ID = 1L;
    private static final String KAKAO_AUTHORIZATION_CODE = "test-kakao-authorization-code";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";
    private static final Instant ACCESS_TOKEN_EXPIRES_AT = Instant.parse("2026-08-21T01:00:00Z");
    private static final Instant REFRESH_TOKEN_EXPIRES_AT = Instant.parse("2026-09-20T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocialLoginFacade socialLoginFacade;

    @Test
    @DisplayName("Access Token 없이 로그인하고 Access Token JSON과 Refresh Token Cookie를 받는다")
    void loginWithoutAccessToken() throws Exception {
        when(socialLoginFacade.login(any())).thenReturn(SocialLoginResult.of(USER_ID, createIssuedTokenPair()));

        mockMvc.perform(post("/api/auth/login")
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
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.authorizationCode").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(
                        "refresh_token=" + REFRESH_TOKEN
                )))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=2592000")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
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
    @DisplayName("authorizationCode가 누락되면 공통 입력 오류를 반환한다")
    void rejectMissingAuthorizationCode() throws Exception {
        expectInvalidInput(mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO"
                                }
                                """)), "authorizationCode");

        verifyNoInteractions(socialLoginFacade);
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
