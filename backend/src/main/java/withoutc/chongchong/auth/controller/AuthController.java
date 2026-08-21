package withoutc.chongchong.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.dto.SocialLoginRequest;
import withoutc.chongchong.auth.dto.SocialLoginResponse;
import withoutc.chongchong.auth.service.SocialLoginFacade;
import withoutc.chongchong.auth.token.IssuedTokenPair;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final SocialLoginFacade socialLoginFacade;

    @PostMapping("/login")
    public ResponseEntity<SocialLoginResponse> login(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        IssuedTokenPair tokenPair = socialLoginFacade.login(request.toCommand());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(SocialLoginResponse.from(tokenPair));
    }
}
