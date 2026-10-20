package withoutc.chongchong.notification.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterRequest;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterResponse;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionStatusResponse;
import withoutc.chongchong.notification.service.WebPushSubscriptionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/web-push-subscriptions")
public class WebPushSubscriptionController {

    private final WebPushSubscriptionService webPushSubscriptionService;

    @PostMapping
    public ResponseEntity<WebPushSubscriptionRegisterResponse> register(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid WebPushSubscriptionRegisterRequest request
    ) {
        WebPushSubscriptionRegisterResponse response = webPushSubscriptionService.register(user.id(), request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<WebPushSubscriptionStatusResponse> getMyStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @NotBlank(message = "installationId는 필수입니다.")
            @Size(max = 255, message = "installationId는 {max}자 이내여야 합니다.")
            String installationId
    ) {
        WebPushSubscriptionStatusResponse response = webPushSubscriptionService.getStatus(
                user.id(),
                installationId
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> deactivate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Positive(message = "웹 푸시 구독 ID는 양수여야 합니다.")
            Long subscriptionId
    ) {
        webPushSubscriptionService.deactivate(user.id(), subscriptionId);

        return ResponseEntity.noContent().build();
    }
}
