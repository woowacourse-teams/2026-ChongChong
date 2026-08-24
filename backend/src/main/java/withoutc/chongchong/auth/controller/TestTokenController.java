package withoutc.chongchong.auth.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.token.AccessTokenIssuer;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class TestTokenController {

    private final AccessTokenIssuer accessTokenIssuer;

    @GetMapping("/token")
    public String issueAccessToken(
            @RequestParam @Positive Long userId
    ) {
        return accessTokenIssuer.issue(userId).value();
    }
}
