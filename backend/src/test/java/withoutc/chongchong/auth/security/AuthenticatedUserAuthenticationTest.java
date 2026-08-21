package withoutc.chongchong.auth.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.support.TestJwtFactory;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthenticatedUserAuthenticationTest.TestController.class)
@ActiveProfiles("test")
class AuthenticatedUserAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtFactory testJwtFactory;

    @Test
    @DisplayName("배포 상태 확인은 인증 없이 호출할 수 있다")
    void allowHealthCheckWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Controller는 인증된 총총 내부 사용자 ID를 전달받는다")
    void provideAuthenticatedUserToController() throws Exception {
        mockMvc.perform(get("/test/authenticated-user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwtFactory.accessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("양의 Long이 아닌 subject를 가진 HTTP 요청은 인증에 실패한다")
    void rejectHttpRequestWithInvalidSubject() throws Exception {
        mockMvc.perform(get("/test/authenticated-user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwtFactory.accessTokenWithSubject("abc")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        not(containsString("NumberFormatException"))
                ))
                .andExpect(content().string(not(containsString("For input string"))));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/authenticated-user")
        Long authenticatedUserId(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
            return authenticatedUser.id();
        }
    }
}
