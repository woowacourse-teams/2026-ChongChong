package withoutc.chongchong.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.dto.SocialLoginRequest;
import withoutc.chongchong.auth.dto.SocialLoginResponse;
import withoutc.chongchong.auth.dto.WebCsrfTokenResponse;
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
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final SocialLoginFacade socialLoginFacade;
    private final AuthTokenService authTokenService;
    private final WebRefreshCookieReader webRefreshCookieReader;
    private final WebRefreshCookieWriter webRefreshCookieWriter;

    @GetMapping("/csrf")
    @Operation(summary = "CSRF 토큰 조회", description = "웹 인증 요청에 사용할 CSRF 토큰을 조회한다.")
    @ApiResponse(responseCode = "200", description = "CSRF 토큰 조회 성공")
    public ResponseEntity<WebCsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(WebCsrfTokenResponse.from(csrfToken));
    }

    @PostMapping("/login")
    @Operation(summary = "소셜 로그인", description = "소셜 로그인 인가 코드로 액세스 토큰을 발급한다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공. Refresh Token은 HttpOnly 쿠키로 발급된다.")
    public ResponseEntity<SocialLoginResponse> login(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        IssuedTokenPair tokenPair = socialLoginFacade.login(request.toCommand());
        return tokenResponse(tokenPair);
    }

    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 갱신", description = "Refresh Token 쿠키를 회전시키고 새로운 액세스 토큰을 발급한다.")
    @ApiResponse(responseCode = "200", description = "토큰 갱신 성공. 새로운 Refresh Token은 HttpOnly 쿠키로 발급된다.")
    public ResponseEntity<SocialLoginResponse> refresh(HttpServletRequest request) {
        IssuedTokenPair tokenPair = webRefreshCookieReader.read(request)
                .map(authTokenService::rotate)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        return tokenResponse(tokenPair);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 폐기하고 인증 쿠키를 만료시킨다.")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
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
