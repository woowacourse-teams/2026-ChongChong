package withoutc.chongchong.notification.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

public class TestNotificationSender implements NotificationSender {

    private final List<ClaimedDelivery> deliveries = new CopyOnWriteArrayList<>();
    private volatile RuntimeException failure;

    @Override
    public WebPushSendResult send(ClaimedDelivery delivery) {
        if (failure != null) {
            throw failure;
        }
        deliveries.add(delivery);
        return WebPushSendResult.SENT;
    }

    public List<ClaimedDelivery> deliveries() {
        return List.copyOf(deliveries);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public void clear() {
        deliveries.clear();
        failure = null;
    }
}
