package withoutc.chongchong.notification.worker;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@Service
@RequiredArgsConstructor
public class DeliveryClaimService {

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final Clock clock;

    @Transactional
    public List<ClaimedDelivery> claimBatch(int batchSize) {
        List<ClaimedDelivery> deliveries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);

        for (NotificationDelivery delivery : notificationDeliveryRepository.findClaimableForUpdate(now, batchSize)) {
            delivery.claim(now);
            deliveries.add(ClaimedDelivery.from(delivery));
        }
        return deliveries;
    }
}
