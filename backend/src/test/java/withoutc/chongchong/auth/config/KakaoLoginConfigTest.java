package withoutc.chongchong.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import withoutc.chongchong.auth.social.kakao.KakaoTokenClient;

class KakaoLoginConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KakaoLoginConfig.class)
            .withPropertyValues(
                    "auth.social.kakao.rest-api-key=test-rest-api-key",
                    "auth.social.kakao.client-secret=test-client-secret",
                    "auth.social.kakao.redirect-uri=http://localhost:3005/auth/kakao/callback",
                    "auth.social.kakao.token-uri=https://kauth.kakao.com/oauth/token",
                    "auth.social.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me",
                    "auth.social.kakao.connect-timeout=2s",
                    "auth.social.kakao.read-timeout=3s"
            );

    @Test
    @DisplayName("Kakao 로그인 설정을 애플리케이션 설정으로 등록한다")
    void registerProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KakaoLoginProperties.class);
            assertThat(context).hasSingleBean(RestClient.class);
            assertThat(context).hasSingleBean(KakaoTokenClient.class);
        });
    }

    @Test
    @DisplayName("Kakao REST API 키가 누락되면 애플리케이션 시작을 실패시킨다")
    void rejectMissingRestApiKeyOnStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(KakaoLoginConfig.class)
                .withPropertyValues(
                        "auth.social.kakao.client-secret=test-client-secret",
                        "auth.social.kakao.redirect-uri=http://localhost:3005/auth/kakao/callback",
                        "auth.social.kakao.token-uri=https://kauth.kakao.com/oauth/token",
                        "auth.social.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me",
                        "auth.social.kakao.connect-timeout=2s",
                        "auth.social.kakao.read-timeout=3s"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("Kakao Client Secret이 공백이면 애플리케이션 시작을 실패시킨다")
    void rejectBlankClientSecretOnStartup() {
        contextRunner
                .withPropertyValues("auth.social.kakao.client-secret= ")
                .run(context -> assertThat(context).hasFailed());
    }
}
