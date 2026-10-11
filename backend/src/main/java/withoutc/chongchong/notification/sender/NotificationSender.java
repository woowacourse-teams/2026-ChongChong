package withoutc.chongchong.notification.sender;

import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

public interface NotificationSender {

    WebPushSendResult send(ClaimedDelivery delivery);
}
