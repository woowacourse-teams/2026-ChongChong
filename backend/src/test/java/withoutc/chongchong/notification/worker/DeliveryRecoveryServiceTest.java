package withoutc.chongchong.notification.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;

@ExtendWith(MockitoExtension.class)
class DeliveryRecoveryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 10, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-26T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    private DeliveryRecoveryService deliveryRecoveryService;

    @BeforeEach
    void setUp() {
        deliveryRecoveryService = new DeliveryRecoveryService(
                notificationDeliveryRepository,
                CLOCK,
                5 * 60 * 1000L
        );
    }

    @Test
    @DisplayName("오래된 PROCESSING 발송 기록을 재시도 대기로 복구한다")
    void recoverStuckProcessingDelivery() {
        NotificationDelivery delivery = createClaimedDelivery(1);
        when(notificationDeliveryRepository.findStuckProcessingForUpdate(NOW.minusMinutes(5), 100))
                .thenReturn(List.of(delivery));

        deliveryRecoveryService.recover(100);

        verify(notificationDeliveryRepository)
                .findStuckProcessingForUpdate(NOW.minusMinutes(5), 100);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RETRY_WAIT);
        assertThat(delivery.getNextRetryAt()).isEqualTo(NOW);
        assertThat(delivery.getLastError()).isEqualTo("PROCESSING 상태가 오래 유지되어 재처리합니다.");
    }

    @Test
    @DisplayName("최대 시도 횟수에 도달한 고착 발송 기록은 실패 처리한다")
    void failStuckProcessingDeliveryWhenMaxAttemptsReached() {
        NotificationDelivery delivery = createDelivery();
        for (int attempt = 1; attempt < DeliveryResultService.MAX_ATTEMPTS; attempt++) {
            LocalDateTime attemptTime = NOW.minusMinutes(10L - attempt);
            delivery.claim(attemptTime);
            delivery.markRetry(attemptTime.plusMinutes(1), "일시적인 실패");
        }
        delivery.claim(NOW.minusMinutes(6));

        when(notificationDeliveryRepository.findStuckProcessingForUpdate(NOW.minusMinutes(5), 100))
                .thenReturn(List.of(delivery));

        deliveryRecoveryService.recover(100);

        assertThat(delivery.getAttemptCount()).isEqualTo(DeliveryResultService.MAX_ATTEMPTS);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getLastError()).isEqualTo("PROCESSING 상태가 오래 유지되어 재처리합니다.");
    }

    private NotificationDelivery createClaimedDelivery(int attemptCount) {
        NotificationDelivery delivery = createDelivery();
        delivery.claim(NOW.minusMinutes(6));
        for (int attempt = 1; attempt < attemptCount; attempt++) {
            delivery.markRetry(NOW, "일시적인 실패");
            delivery.claim(NOW);
        }
        return delivery;
    }

    private NotificationDelivery createDelivery() {
        return NotificationDelivery.create(mock(Notification.class), mock(WebPushSubscription.class));
    }
}
