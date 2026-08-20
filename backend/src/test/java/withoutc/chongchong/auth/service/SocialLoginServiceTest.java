package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.entity.SocialAccount;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class SocialLoginServiceTest {

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("처음 로그인한 소셜 사용자를 자동 가입시키고 총총 Token을 발급한다")
    void signUpNewSocialUserAndIssueTokenPair() {
        SocialUserInfo socialUserInfo = createSocialUserInfo(
                SocialProvider.GOOGLE,
                "google-user-id",
                "총총이",
                "https://example.com/profile.png"
        );

        IssuedTokenPair tokenPair = socialLoginService.login(socialUserInfo);

        User user = userRepository.findAll().getFirst();
        SocialAccount socialAccount = socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.GOOGLE,
                "google-user-id"
        ).orElseThrow();
        AuthSession authSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        Jwt accessToken = jwtDecoder.decode(tokenPair.accessToken().value());
        HashedRefreshToken expectedRefreshTokenHash = refreshTokenHasher.hash(tokenPair.refreshToken());

        assertThat(userRepository.count()).isOne();
        assertThat(user.getName()).isEqualTo("총총이");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(socialAccount.getUser().getId()).isEqualTo(user.getId());
        assertThat(authSessionRepository.count()).isOne();
        assertThat(authSession.getRefreshTokenHash()).isEqualTo(expectedRefreshTokenHash);
        assertThat(accessToken.getSubject()).isEqualTo(user.getId().toString());
    }

    @Test
    @DisplayName("기존 소셜 사용자는 연결된 User를 재사용하고 프로필을 덮어쓰지 않는다")
    void reuseExistingUserWithoutUpdatingProfile() {
        User existingUser = userRepository.saveAndFlush(User.create(
                "기존 이름",
                "https://example.com/old-profile.png"
        ));
        socialAccountRepository.saveAndFlush(SocialAccount.create(
                existingUser,
                SocialProvider.GOOGLE,
                "google-user-id"
        ));
        SocialUserInfo changedSocialUserInfo = createSocialUserInfo(
                SocialProvider.GOOGLE,
                "google-user-id",
                "Provider에서 바뀐 이름",
                "https://example.com/new-profile.png"
        );

        IssuedTokenPair tokenPair = socialLoginService.login(changedSocialUserInfo);

        User found = userRepository.findById(existingUser.getId()).orElseThrow();
        Jwt accessToken = jwtDecoder.decode(tokenPair.accessToken().value());
        assertThat(userRepository.count()).isOne();
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(authSessionRepository.count()).isOne();
        assertThat(found.getName()).isEqualTo("기존 이름");
        assertThat(found.getProfileImageUrl()).isEqualTo("https://example.com/old-profile.png");
        assertThat(accessToken.getSubject()).isEqualTo(existingUser.getId().toString());
    }

    @Test
    @DisplayName("같은 소셜 사용자가 다시 로그인하면 기존 인증 세션을 교체한다")
    void replaceAuthSessionOnSocialRelogin() {
        SocialUserInfo socialUserInfo = createSocialUserInfo(
                SocialProvider.GOOGLE,
                "google-user-id",
                "총총이",
                null
        );
        IssuedTokenPair firstTokenPair = socialLoginService.login(socialUserInfo);
        User user = userRepository.findAll().getFirst();
        AuthSession firstSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        Long firstSessionId = firstSession.getId();
        HashedRefreshToken firstRefreshTokenHash = firstSession.getRefreshTokenHash();

        IssuedTokenPair secondTokenPair = socialLoginService.login(socialUserInfo);

        AuthSession replacedSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(userRepository.count()).isOne();
        assertThat(socialAccountRepository.count()).isOne();
        assertThat(authSessionRepository.count()).isOne();
        assertThat(replacedSession.getId()).isEqualTo(firstSessionId);
        assertThat(replacedSession.getRefreshTokenHash()).isNotEqualTo(firstRefreshTokenHash);
        assertThat(secondTokenPair.refreshToken()).isNotEqualTo(firstTokenPair.refreshToken());
    }

    @Test
    @DisplayName("제공자가 다르면 같은 제공자 사용자 ID를 서로 다른 총총 사용자로 가입시킨다")
    void distinguishSameProviderUserIdFromDifferentProviders() {
        IssuedTokenPair googleTokenPair = socialLoginService.login(createSocialUserInfo(
                SocialProvider.GOOGLE,
                "same-provider-user-id",
                "Google 사용자",
                null
        ));
        IssuedTokenPair kakaoTokenPair = socialLoginService.login(createSocialUserInfo(
                SocialProvider.KAKAO,
                "same-provider-user-id",
                "Kakao 사용자",
                null
        ));

        String googleUserId = jwtDecoder.decode(googleTokenPair.accessToken().value()).getSubject();
        String kakaoUserId = jwtDecoder.decode(kakaoTokenPair.accessToken().value()).getSubject();
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(socialAccountRepository.count()).isEqualTo(2);
        assertThat(authSessionRepository.count()).isEqualTo(2);
        assertThat(googleUserId).isNotEqualTo(kakaoUserId);
    }

    @Test
    @DisplayName("검증된 소셜 사용자 정보가 없으면 로그인할 수 없다")
    void rejectNullSocialUserInfo() {
        assertThatThrownBy(() -> socialLoginService.login(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검증된 소셜 사용자 정보는 필수입니다.");

        assertThat(userRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
        assertThat(authSessionRepository.count()).isZero();
    }

    private SocialUserInfo createSocialUserInfo(
            SocialProvider provider,
            String providerUserId,
            String displayName,
            String profileImageUrl
    ) {
        return new SocialUserInfo(provider, providerUserId, displayName, profileImageUrl);
    }
}
