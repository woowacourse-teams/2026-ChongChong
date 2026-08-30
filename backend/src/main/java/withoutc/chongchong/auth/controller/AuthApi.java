package withoutc.chongchong.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import withoutc.chongchong.auth.dto.SocialLoginRequest;
import withoutc.chongchong.auth.dto.SocialLoginResponse;
import withoutc.chongchong.auth.dto.WebCsrfTokenResponse;

@Tag(name = "Auth", description = "인증 API")
public interface AuthApi {

    @Operation(summary = "CSRF 토큰 조회", description = "웹 인증 요청에 사용할 CSRF 토큰을 조회한다.")
    @ApiResponse(responseCode = "200", description = "CSRF 토큰 조회 성공")
    ResponseEntity<WebCsrfTokenResponse> csrf(CsrfToken csrfToken);

    @Operation(summary = "소셜 로그인", description = "소셜 로그인 인가 코드로 액세스 토큰을 발급한다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공. Refresh Token은 HttpOnly 쿠키로 발급된다.")
    ResponseEntity<SocialLoginResponse> login(@Valid SocialLoginRequest request);

    @Operation(summary = "액세스 토큰 갱신", description = "Refresh Token 쿠키를 회전시키고 새로운 액세스 토큰을 발급한다.")
    @ApiResponse(responseCode = "200", description = "토큰 갱신 성공. 새로운 Refresh Token은 HttpOnly 쿠키로 발급된다.")
    ResponseEntity<SocialLoginResponse> refresh(HttpServletRequest request);

    @Operation(summary = "로그아웃", description = "Refresh Token을 폐기하고 인증 쿠키를 만료시킨다.")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
    ResponseEntity<Void> logout(HttpServletRequest request);
}
