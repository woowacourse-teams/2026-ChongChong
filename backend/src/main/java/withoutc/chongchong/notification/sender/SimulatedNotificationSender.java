package withoutc.chongchong.notification.sender;

import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

public class SimulatedNotificationSender implements NotificationSender {

    private final long delayMs;

    public SimulatedNotificationSender(long delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("알림 시뮬레이터의 지연 시간은 0ms 이상이어야 합니다.");
        }
        this.delayMs = delayMs;
    }

    @Override
    public WebPushSendResult send(ClaimedDelivery delivery) {
        try {
            Thread.sleep(delayMs);
            return WebPushSendResult.SENT;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_INTERRUPTED);
        }
    }
}
