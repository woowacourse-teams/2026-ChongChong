package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceValidationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    @DisplayName("검증된 소셜 사용자 정보가 없으면 저장소 조회나 Token 발급 전에 거부한다")
    void rejectNullSocialUserInfo() {
        assertThatThrownBy(() -> socialLoginService.login(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검증된 소셜 사용자 정보는 필수입니다.");

        verifyNoInteractions(userRepository, socialAccountRepository, authTokenService);
    }
}
