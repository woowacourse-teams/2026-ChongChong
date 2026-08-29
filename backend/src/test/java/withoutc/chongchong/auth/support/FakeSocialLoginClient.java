package withoutc.chongchong.auth.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.social.SocialLoginClient;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.social.SocialUserInfo;

public final class FakeSocialLoginClient implements SocialLoginClient {

    private final SocialProvider provider;
    private final Map<String, SocialUserInfo> successfulResponses = new ConcurrentHashMap<>();

    public FakeSocialLoginClient(SocialProvider provider) {
        this.provider = provider;
    }

    public void willSucceed(String credential, SocialUserInfo socialUserInfo) {
        successfulResponses.put(credential, socialUserInfo);
    }

    public void willFail(String credential) {
        successfulResponses.remove(credential);
    }

    public void clear() {
        successfulResponses.clear();
    }

    @Override
    public SocialProvider provider() {
        return provider;
    }

    @Override
    public SocialUserInfo authenticate(SocialLoginCommand command) {
        SocialUserInfo socialUserInfo = successfulResponses.get(command.credential());
        if (socialUserInfo == null) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
        }
        return socialUserInfo;
    }
}
