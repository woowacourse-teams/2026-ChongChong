package withoutc.chongchong.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.auth.entity.SocialAccount;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthTokenService authTokenService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public IssuedTokenPair login(SocialUserInfo socialUserInfo) {
        validateSocialUserInfo(socialUserInfo);
        User user = findOrCreateUser(socialUserInfo);
        return authTokenService.issue(user.getId());
    }

    private User findOrCreateUser(SocialUserInfo socialUserInfo) {
        return socialAccountRepository.findByProviderAndProviderUserId(
                        socialUserInfo.provider(),
                        socialUserInfo.providerUserId()
                )
                .map(SocialAccount::getUser)
                .orElseGet(() -> createUserWithSocialAccount(socialUserInfo));
    }

    private User createUserWithSocialAccount(SocialUserInfo socialUserInfo) {
        User user = userRepository.save(User.create(
                socialUserInfo.displayName(),
                socialUserInfo.profileImageUrl()
        ));
        socialAccountRepository.saveAndFlush(SocialAccount.create(
                user,
                socialUserInfo.provider(),
                socialUserInfo.providerUserId()
        ));
        return user;
    }

    private void validateSocialUserInfo(SocialUserInfo socialUserInfo) {
        if (socialUserInfo == null) {
            throw new IllegalArgumentException("검증된 소셜 사용자 정보는 필수입니다.");
        }
    }
}
