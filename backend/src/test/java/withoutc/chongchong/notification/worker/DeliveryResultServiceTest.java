package withoutc.chongchong.notification.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class DeliveryResultServiceTest {

    private static final Long DELIVERY_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 2L;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    private DeliveryResultService deliveryResultService;

    @BeforeEach
    void setUp() {
        deliveryResultService = new DeliveryResultService(
                notificationDeliveryRepository,
                webPushSubscriptionRepository,
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        );
        when(notificationDeliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("알림 삭제로 발송 기록이 사라진 뒤에도 결과 처리를 마치고 만료 구독은 비활성화한다")
    void completeResultHandlingWhenDeliveryWasDeletedWithNotification() {
        WebPushSubscription subscription = WebPushSubscription.create(
                User.create("수신자", null),
                "installation-id",
                "https://push.example.com/subscription",
                "p256dh-key",
                "auth-secret"
        );
        when(webPushSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        deliveryResultService.markSent(DELIVERY_ID);
        assertThat(deliveryResultService.markRetry(DELIVERY_ID, "재시도 오류")).isFalse();
        deliveryResultService.markExpired(DELIVERY_ID, SUBSCRIPTION_ID, "구독 만료");
        deliveryResultService.markFailed(DELIVERY_ID, "발송 실패");

        assertThat(subscription.isActive()).isFalse();
        verify(notificationDeliveryRepository, times(4)).findById(DELIVERY_ID);
    }

    @Test
    @DisplayName("탈퇴 후 발송 기록과 구독이 모두 삭제되어도 만료 결과 처리를 마친다")
    void completeExpiredResultHandlingWhenDeliveryAndSubscriptionWereDeleted() {
        when(webPushSubscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> deliveryResultService.markExpired(DELIVERY_ID, SUBSCRIPTION_ID, "구독 만료"))
                .doesNotThrowAnyException();

        verify(notificationDeliveryRepository).findById(DELIVERY_ID);
        verify(webPushSubscriptionRepository).findById(SUBSCRIPTION_ID);
    }
}
