package withoutc.chongchong.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.http.WebRefreshCookie;
import withoutc.chongchong.auth.http.WebRefreshCookieWriter;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.user.controller.dto.UserProfileResponse;
import withoutc.chongchong.user.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final WebRefreshCookieWriter webRefreshCookieWriter;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(userService.getMyProfile(user.id()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal AuthenticatedUser user) {
        userService.withdraw(user.id());

        WebRefreshCookie expiredRefreshCookie = webRefreshCookieWriter.expire();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie.headerValue())
                .build();
    }
}
