package withoutc.chongchong.notification.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.service.NotificationReminderService;

@Component
@RequiredArgsConstructor
public class NotificationReminderWorker {

    private final NotificationReminderService notificationReminderService;

    @Scheduled(fixedDelayString = "${notification.reminder.fixed-delay-ms:60000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process() {
        notificationReminderService.findCandidates().forEach(notificationReminderService::process);
    }
}
