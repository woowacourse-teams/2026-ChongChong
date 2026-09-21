package withoutc.chongchong.notification.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.notification.sender.NotificationSender;

public class TestNotificationSender implements NotificationSender {

    private final List<NotificationEvent> events = new CopyOnWriteArrayList<>();
    private volatile RuntimeException failure;

    @Override
    public void sendNotifications(NotificationEvent event) {
        if (failure != null) {
            throw failure;
        }
        events.add(event);
    }

    public List<NotificationEvent> events() {
        return List.copyOf(events);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public void clear() {
        events.clear();
        failure = null;
    }
}
