package withoutc.chongchong.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.notification.sender.NotificationSender;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSender notificationSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handle(NotificationEvent event) {
        try {
            notificationSender.sendNotifications(event);
        } catch (RestClientException exception) {
            log.error("알림 전송에 실패했습니다.", exception);
        }
    }
}
