package withoutc.chongchong.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.service.SocialLoginFacade;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.token.IssuedAccessToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String GOOGLE_ID_TOKEN = "test-google-id-token";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";
    private static final Instant ACCESS_TOKEN_EXPIRES_AT = Instant.parse("2026-08-21T01:00:00Z");
    private static final Instant REFRESH_TOKEN_EXPIRES_AT = Instant.parse("2026-09-20T01:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocialLoginFacade socialLoginFacade;

    @Test
    @DisplayName("Access Token 없이 소셜 로그인하고 Token 쌍을 JSON으로 받는다")
    void loginWithoutAccessToken() throws Exception {
        when(socialLoginFacade.login(any())).thenReturn(createIssuedTokenPair());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "idToken": "%s"
                                }
                                """.formatted(GOOGLE_ID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.accessTokenExpiresAt").value("2026-08-21T01:00:00Z"))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN))
                .andExpect(jsonPath("$.refreshTokenExpiresAt").value("2026-09-20T01:00:00Z"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.idToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(socialLoginFacade).login(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                GOOGLE_ID_TOKEN
        ));
    }

    @Test
    @DisplayName("provider가 누락되면 공통 입력 오류를 반환한다")
    void rejectMissingProvider() throws Exception {
        expectInvalidInput(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "%s"
                                }
                                """.formatted(GOOGLE_ID_TOKEN))), "provider")
                .andExpect(content().string(not(containsString(GOOGLE_ID_TOKEN))));

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("idToken이 누락되면 공통 입력 오류를 반환한다")
    void rejectMissingIdToken() throws Exception {
        expectInvalidInput(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE"
                                }
                                """)), "idToken");

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("idToken이 공백이면 공통 입력 오류를 반환한다")
    void rejectBlankIdToken() throws Exception {
        expectInvalidInput(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "idToken": " "
                                }
                                """)), "idToken");

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("알 수 없는 provider 문자열이면 공통 잘못된 요청 오류를 반환한다")
    void rejectUnknownProvider() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "UNKNOWN",
                                  "idToken": "%s"
                                }
                                """.formatted(GOOGLE_ID_TOKEN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 형식이 잘못되었습니다."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(content().string(not(containsString(GOOGLE_ID_TOKEN))));

        verifyNoInteractions(socialLoginFacade);
    }

    @Test
    @DisplayName("등록되지 않은 provider이면 공통 Auth 오류를 반환한다")
    void rejectUnsupportedProvider() throws Exception {
        when(socialLoginFacade.login(any()))
                .thenThrow(new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));

        expectAuthError(
                mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "KAKAO",
                                  "idToken": "%s"
                                }
                                """.formatted(GOOGLE_ID_TOKEN))),
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "idToken": "%s"
                                }
                                """.formatted(GOOGLE_ID_TOKEN))),
                401,
                "SOCIAL_AUTHENTICATION_FAILED",
                "소셜 로그인 인증에 실패했습니다."
        );
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
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
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
                .andExpect(content().string(not(containsString(GOOGLE_ID_TOKEN))))
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
}
