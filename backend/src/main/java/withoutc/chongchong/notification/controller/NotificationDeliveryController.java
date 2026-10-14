package withoutc.chongchong.notification.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.notification.controller.dto.NotificationDeliveryClickRequest;
import withoutc.chongchong.notification.service.NotificationDeliveryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification-deliveries")
public class NotificationDeliveryController {

    private final NotificationDeliveryService notificationDeliveryService;

    @PostMapping("/{deliveryId}/click")
    public ResponseEntity<Void> click(
            @PathVariable
            @Positive(message = "알림 발송 내역 ID는 양수여야 합니다.")
            Long deliveryId,
            @Valid @RequestBody NotificationDeliveryClickRequest request
    ) {
        notificationDeliveryService.click(deliveryId, request.clickToken());

        return ResponseEntity
                .noContent()
                .build();
    }
}
