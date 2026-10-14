package withoutc.chongchong.notification.metrics;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.projection.ClaimableQueueSnapshotProjection;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryQueueMetricsUpdaterTest {

    private static final Instant NOW = Instant.parse("2026-10-14T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private NotificationDeliveryQueueMetrics notificationDeliveryQueueMetrics;

    private NotificationDeliveryQueueMetricsUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new NotificationDeliveryQueueMetricsUpdater(
                notificationDeliveryRepository,
                notificationDeliveryQueueMetrics,
                CLOCK
        );
    }

    @Test
    @DisplayName("큐 스냅샷 조회에 성공하면 마지막 성공 시각을 기록한다")
    void recordLastSuccessfulRefreshAfterSnapshotQuerySucceeds() {
        when(notificationDeliveryRepository.findClaimableQueueSnapshot(
                LocalDateTime.now(CLOCK), DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAIT
        )).thenReturn(new ClaimableQueueSnapshotProjection(0, null));

        updater.refresh();

        verify(notificationDeliveryQueueMetrics).clearQueueSnapshot(CLOCK.millis());
    }

    @Test
    @DisplayName("큐에 작업이 있으면 가장 오래 기다린 작업의 경과 시간을 기록한다")
    void recordOldestReadyAgeWhenQueueHasReadyDeliveries() {
        LocalDateTime oldestReadyAt = LocalDateTime.now(CLOCK).minusMinutes(2);
        when(notificationDeliveryRepository.findClaimableQueueSnapshot(
                LocalDateTime.now(CLOCK), DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAIT
        )).thenReturn(new ClaimableQueueSnapshotProjection(2, oldestReadyAt));

        updater.refresh();

        verify(notificationDeliveryQueueMetrics).updateQueueSnapshot(
                2, Duration.between(oldestReadyAt, LocalDateTime.now(CLOCK)), CLOCK.millis());
    }

    @Test
    @DisplayName("큐 스냅샷 조회에 실패하면 마지막 성공 시각을 갱신하지 않는다")
    void keepLastSuccessfulRefreshWhenSnapshotQueryFails() {
        when(notificationDeliveryRepository.findClaimableQueueSnapshot(
                LocalDateTime.now(CLOCK), DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAIT
        )).thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThatThrownBy(updater::refresh)
                .isInstanceOf(DataAccessResourceFailureException.class);

        verifyNoInteractions(notificationDeliveryQueueMetrics);
    }
}
