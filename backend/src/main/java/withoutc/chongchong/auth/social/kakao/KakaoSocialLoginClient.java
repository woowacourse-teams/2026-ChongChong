package withoutc.chongchong.auth.social.kakao;

import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.social.SocialLoginClient;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse.KakaoAccount;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse.KakaoProfile;

public class KakaoSocialLoginClient implements SocialLoginClient {

    private final KakaoTokenClient tokenClient;
    private final KakaoUserInfoClient userInfoClient;

    public KakaoSocialLoginClient(
            KakaoTokenClient tokenClient,
            KakaoUserInfoClient userInfoClient
    ) {
        this.tokenClient = tokenClient;
        this.userInfoClient = userInfoClient;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public SocialUserInfo authenticate(SocialLoginCommand command) {
        validateCommand(command);

        KakaoAccessToken accessToken = tokenClient.exchange(command.credential());
        KakaoUserInfoResponse response = userInfoClient.fetch(accessToken);
        return toSocialUserInfo(response);
    }

    private SocialUserInfo toSocialUserInfo(KakaoUserInfoResponse response) {
        try {
            Long providerUserId = requireProviderUserId(response);
            KakaoProfile profile = requireProfile(response.kakaoAccount());
            return new SocialUserInfo(
                    SocialProvider.KAKAO,
                    providerUserId.toString(),
                    profile.nickname(),
                    profile.profileImageUrl()
            );
        } catch (IllegalArgumentException exception) {
            throw authenticationFailed();
        }
    }

    private Long requireProviderUserId(KakaoUserInfoResponse response) {
        if (response == null || response.id() == null || response.id() <= 0) {
            throw new IllegalArgumentException("Kakao 회원번호가 올바르지 않습니다.");
        }
        return response.id();
    }

    private KakaoProfile requireProfile(KakaoAccount account) {
        if (account == null || account.profile() == null) {
            throw new IllegalArgumentException("Kakao 프로필이 없습니다.");
        }
        return account.profile();
    }

    private void validateCommand(SocialLoginCommand command) {
        if (command == null || command.provider() != SocialProvider.KAKAO) {
            throw authenticationFailed();
        }
    }

    private AuthException authenticationFailed() {
        return new AuthException(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
    }
}
