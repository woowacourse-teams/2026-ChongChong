package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(SocialLoginServiceRollbackTest.FailingAuthTokenServiceConfig.class)
class SocialLoginServiceRollbackTest {

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    @Qualifier("failingAuthTokenService")
    private AuthTokenService failingAuthTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("Token 발급이 실패하면 신규 User와 SocialAccount를 함께 롤백한다")
    void rollbackUserAndSocialAccountWhenTokenIssueFails() {
        AtomicBoolean transactionActiveDuringTokenIssue = new AtomicBoolean();
        when(failingAuthTokenService.issue(anyLong())).thenAnswer(invocation -> {
            transactionActiveDuringTokenIssue.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            throw new IllegalStateException("Token 발급 실패");
        });
        SocialUserInfo socialUserInfo = new SocialUserInfo(
                SocialProvider.GOOGLE,
                "google-user-id",
                "총총이",
                null
        );

        assertThatThrownBy(() -> socialLoginService.login(socialUserInfo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Token 발급 실패");

        assertThat(transactionActiveDuringTokenIssue).isTrue();
        assertThat(userRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
        assertThat(authSessionRepository.count()).isZero();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingAuthTokenServiceConfig {

        @Bean
        @Primary
        AuthTokenService failingAuthTokenService() {
            return mock(AuthTokenService.class);
        }
    }
}
