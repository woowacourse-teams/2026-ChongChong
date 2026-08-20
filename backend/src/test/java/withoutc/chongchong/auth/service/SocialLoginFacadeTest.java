package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.social.SocialLoginClient;
import withoutc.chongchong.auth.social.SocialLoginClients;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.support.FakeSocialLoginClient;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class SocialLoginFacadeTest {

    @Autowired
    private SocialLoginService socialLoginService;

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
    @DisplayName("Provider 인증은 DB Transaction 밖에서 수행하고 검증 결과로 내부 로그인을 처리한다")
    void authenticateProviderOutsideTransactionAndLogin() {
        String authorizationCode = "valid-authorization-code";
        FakeSocialLoginClient fakeClient = new FakeSocialLoginClient(SocialProvider.GOOGLE);
        fakeClient.willSucceed(authorizationCode, new SocialUserInfo(
                SocialProvider.GOOGLE,
                "google-user-id",
                "총총이",
                null
        ));
        TransactionRecordingSocialLoginClient recordingClient =
                new TransactionRecordingSocialLoginClient(fakeClient);
        SocialLoginFacade facade = createFacade(recordingClient);

        IssuedTokenPair tokenPair = facade.login(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                authorizationCode
        ));

        assertThat(recordingClient.wasTransactionActiveDuringAuthentication()).isFalse();
        assertThat(tokenPair).isNotNull();
        assertThat(userRepository.count()).isOne();
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(authSessionRepository.count()).isOne();
    }

    @Test
    @DisplayName("Provider 인증에 실패하면 내부 로그인 데이터가 생성되지 않는다")
    void keepDatabaseUnchangedWhenProviderAuthenticationFails() {
        String authorizationCode = "invalid-authorization-code";
        FakeSocialLoginClient fakeClient = new FakeSocialLoginClient(SocialProvider.GOOGLE);
        fakeClient.willFail(authorizationCode);
        TransactionRecordingSocialLoginClient recordingClient =
                new TransactionRecordingSocialLoginClient(fakeClient);
        SocialLoginFacade facade = createFacade(recordingClient);

        assertThatThrownBy(() -> facade.login(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                authorizationCode
        )))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);

        assertThat(recordingClient.wasTransactionActiveDuringAuthentication()).isFalse();
        assertThat(userRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
        assertThat(authSessionRepository.count()).isZero();
    }

    private SocialLoginFacade createFacade(SocialLoginClient socialLoginClient) {
        return new SocialLoginFacade(
                new SocialLoginClients(List.of(socialLoginClient)),
                socialLoginService
        );
    }

    private static final class TransactionRecordingSocialLoginClient
            implements SocialLoginClient {

        private final SocialLoginClient delegate;
        private boolean transactionActiveDuringAuthentication;

        private TransactionRecordingSocialLoginClient(SocialLoginClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public SocialProvider provider() {
            return delegate.provider();
        }

        @Override
        public SocialUserInfo authenticate(SocialLoginCommand command) {
            transactionActiveDuringAuthentication = TransactionSynchronizationManager
                    .isActualTransactionActive();
            return delegate.authenticate(command);
        }

        private boolean wasTransactionActiveDuringAuthentication() {
            return transactionActiveDuringAuthentication;
        }
    }
}
