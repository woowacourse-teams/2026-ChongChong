package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.auth.entity.SocialAccount;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.token.IssuedAccessToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    @DisplayName("신규 소셜 사용자의 프로필과 계정을 저장하고 저장된 사용자 ID로 토큰을 발급한다")
    void signUpNewSocialUserAndIssueTokenPair() {
        SocialUserInfo info = new SocialUserInfo(SocialProvider.GOOGLE, "google-user-id",
                "총총이", "https://example.com/profile.png");
        when(socialAccountRepository.findByProviderAndProviderUserId(info.provider(), info.providerUserId()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42L);
            return saved;
        });
        IssuedTokenPair tokenPair = tokenPair();
        when(authTokenService.issue(42L)).thenReturn(tokenPair);

        SocialLoginResult result = socialLoginService.login(info);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo(info.displayName());
        assertThat(savedUser.getProfileImageUrl()).isEqualTo(info.profileImageUrl());
        ArgumentCaptor<SocialAccount> accountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).saveAndFlush(accountCaptor.capture());
        SocialAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUser()).isSameAs(savedUser);
        assertThat(savedAccount.getProvider()).isEqualTo(info.provider());
        assertThat(savedAccount.getProviderUserId()).isEqualTo(info.providerUserId());
        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.tokenPair()).isSameAs(tokenPair);
        verify(authTokenService).issue(42L);
    }

    @Test
    @DisplayName("기존 소셜 사용자는 추가 가입이나 프로필 덮어쓰기 없이 연결된 사용자로 토큰을 발급한다")
    void reuseExistingUserWithoutUpdatingProfile() {
        User existingUser = User.create("기존 이름", "https://example.com/old-profile.png");
        ReflectionTestUtils.setField(existingUser, "id", 42L);
        SocialUserInfo info = new SocialUserInfo(SocialProvider.GOOGLE, "google-user-id",
                "변경된 이름", "https://example.com/new-profile.png");
        when(socialAccountRepository.findByProviderAndProviderUserId(info.provider(), info.providerUserId()))
                .thenReturn(Optional.of(SocialAccount.create(existingUser, info.provider(), info.providerUserId())));
        IssuedTokenPair tokenPair = tokenPair();
        when(authTokenService.issue(42L)).thenReturn(tokenPair);

        SocialLoginResult result = socialLoginService.login(info);

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.tokenPair()).isSameAs(tokenPair);
        assertThat(existingUser.getName()).isEqualTo("기존 이름");
        assertThat(existingUser.getProfileImageUrl()).isEqualTo("https://example.com/old-profile.png");
        verifyNoInteractions(userRepository);
        verify(socialAccountRepository, never()).saveAndFlush(any());
        verify(authTokenService).issue(42L);
    }

    private IssuedTokenPair tokenPair() {
        return new IssuedTokenPair(
                new IssuedAccessToken("access-token", Instant.parse("2026-11-05T01:00:00Z")),
                new RawRefreshToken("refresh-token"), Instant.parse("2026-12-05T00:00:00Z"));
    }

    @Test
    @DisplayName("검증된 소셜 사용자 정보가 없으면 저장소 조회나 Token 발급 전에 거부한다")
    void rejectNullSocialUserInfo() {
        assertThatThrownBy(() -> socialLoginService.login(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검증된 소셜 사용자 정보는 필수입니다.");

        verifyNoInteractions(userRepository, socialAccountRepository, authTokenService);
    }
}
