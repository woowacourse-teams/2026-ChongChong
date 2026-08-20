package withoutc.chongchong.auth.social;

public interface SocialLoginClient {

    SocialProvider provider();

    SocialUserInfo authenticate(SocialLoginCommand command);
}
