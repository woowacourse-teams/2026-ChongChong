package withoutc.chongchong.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import withoutc.chongchong.auth.social.SocialLoginClients;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialUserInfo;
import withoutc.chongchong.auth.token.IssuedTokenPair;

@Service
@RequiredArgsConstructor
public class SocialLoginFacade {

    private final SocialLoginClients socialLoginClients;
    private final SocialLoginService socialLoginService;

    public IssuedTokenPair login(SocialLoginCommand command) {
        SocialUserInfo socialUserInfo = socialLoginClients.authenticate(command);
        return socialLoginService.login(socialUserInfo);
    }
}
