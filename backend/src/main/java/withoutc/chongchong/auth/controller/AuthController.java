package withoutc.chongchong.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.dto.SocialLoginRequest;
import withoutc.chongchong.auth.dto.SocialLoginResponse;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.http.WebRefreshCookie;
import withoutc.chongchong.auth.http.WebRefreshCookieReader;
import withoutc.chongchong.auth.http.WebRefreshCookieWriter;
import withoutc.chongchong.auth.service.AuthTokenService;
import withoutc.chongchong.auth.service.SocialLoginFacade;
import withoutc.chongchong.auth.token.IssuedTokenPair;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final SocialLoginFacade socialLoginFacade;
    private final AuthTokenService authTokenService;
    private final WebRefreshCookieReader webRefreshCookieReader;
    private final WebRefreshCookieWriter webRefreshCookieWriter;

    @PostMapping("/login")
    public ResponseEntity<SocialLoginResponse> login(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        IssuedTokenPair tokenPair = socialLoginFacade.login(request.toCommand());
        return tokenResponse(tokenPair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<SocialLoginResponse> refresh(HttpServletRequest request) {
        IssuedTokenPair tokenPair = webRefreshCookieReader.read(request)
                .map(authTokenService::rotate)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        return tokenResponse(tokenPair);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        webRefreshCookieReader.read(request)
                .ifPresent(authTokenService::logout);
        WebRefreshCookie expiredRefreshCookie = webRefreshCookieWriter.expire();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie.headerValue())
                .build();
    }

    private ResponseEntity<SocialLoginResponse> tokenResponse(IssuedTokenPair tokenPair) {
        WebRefreshCookie refreshCookie = webRefreshCookieWriter.issue(
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.headerValue())
                .body(SocialLoginResponse.from(tokenPair));
    }
}
