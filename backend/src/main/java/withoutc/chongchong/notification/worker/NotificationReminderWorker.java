package withoutc.chongchong.notification.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import withoutc.chongchong.notification.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationReminderWorker {

    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${notification.reminder.fixed-delay-ms:60000}")
    public void process() {
        notificationService.createScheduledRemindNotifications();
    }
}
