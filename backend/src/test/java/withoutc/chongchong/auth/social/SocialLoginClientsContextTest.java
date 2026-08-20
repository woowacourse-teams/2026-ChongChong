package withoutc.chongchong.auth.social;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SocialLoginClientsContextTest {

    @Autowired
    private SocialLoginClients socialLoginClients;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("실제 Provider Client나 Fake Bean 없이도 운영 구성이 시작된다")
    void startWithoutSocialLoginClient() {
        assertThat(socialLoginClients).isNotNull();
        assertThat(applicationContext.getBeansOfType(SocialLoginClient.class)).isEmpty();
    }
}
