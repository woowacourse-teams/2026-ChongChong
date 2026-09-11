package withoutc.chongchong.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationDeliveryTest {

    @Test
    @DisplayName("알림과 푸시 토큰으로 발송 기록을 생성하면 발송 대기 상태로 생성된다")
    void createPendingDelivery() {
        Notification notification = mock(Notification.class);
        PushToken pushToken = mock(PushToken.class);

        NotificationDelivery delivery = NotificationDelivery.create(notification, pushToken);

        assertThat(delivery.getNotification()).isSameAs(notification);
        assertThat(delivery.getPushToken()).isSameAs(pushToken);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.getAttemptCount()).isZero();
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.getLastError()).isNull();
    }
}
