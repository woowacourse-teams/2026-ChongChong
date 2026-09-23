package withoutc.chongchong.notification.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.notification.controller.dto.MyNotificationListResponse;
import withoutc.chongchong.notification.service.NotificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<MyNotificationListResponse> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MyNotificationListResponse response = notificationService.getMyNotifications(user.id());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<Void> readNotification(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Positive(message = "알림 ID는 양수여야 합니다.")
            Long notificationId
    ) {
        notificationService.readNotification(user.id(), notificationId);

        return ResponseEntity.noContent().build();
    }
}
