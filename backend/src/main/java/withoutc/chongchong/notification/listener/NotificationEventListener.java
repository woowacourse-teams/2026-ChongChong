package withoutc.chongchong.notification.listener;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.notification.sender.NotificationSender;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final Set<NotificationType> DISCORD_SUPPORTED_TYPES =
            Set.of(NotificationType.CREATED, NotificationType.SUBMITTED);

    private final NotificationSender notificationSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handle(NotificationEvent event) {
        if (!DISCORD_SUPPORTED_TYPES.contains(event.type())) {
            return;
        }

        try {
            notificationSender.sendNotifications(event);
        } catch (RestClientException exception) {
            log.error("알림 전송에 실패했습니다.", exception);
        }
    }
}
