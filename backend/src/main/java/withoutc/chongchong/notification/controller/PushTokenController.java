package withoutc.chongchong.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.notification.controller.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.service.PushTokenService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/push-tokens")
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @PostMapping
    public ResponseEntity<Void> registerPushToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid PushTokenCreateRequest request
    ) {
        pushTokenService.registerPushToken(user.id(), request);

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/{installationId}")
    public ResponseEntity<Void> deactivatePushToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String installationId
    ) {
        pushTokenService.deactivatePushToken(user.id(), installationId);

        return ResponseEntity
                .noContent()
                .build();
    }
}
