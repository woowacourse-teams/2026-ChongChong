package withoutc.chongchong.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;

class NotificationDeliveryTest {

    private static final LocalDateTime CLAIMED_AT = LocalDateTime.of(2026, 9, 25, 10, 0);
    private static final LocalDateTime NEXT_RETRY_AT = CLAIMED_AT.plusMinutes(1);
    private static final LocalDateTime SENT_AT = CLAIMED_AT.plusMinutes(2);

    @Test
    @DisplayName("알림과 Web Push 구독으로 발송 기록을 생성하면 발송 대기 상태로 생성된다")
    void createPendingDelivery() {
        Notification notification = mock(Notification.class);
        WebPushSubscription webPushSubscription = mock(WebPushSubscription.class);

        NotificationDelivery delivery = NotificationDelivery.create(notification, webPushSubscription);

        assertThat(delivery.getNotification()).isSameAs(notification);
        assertThat(delivery.getWebPushSubscription()).isSameAs(webPushSubscription);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.getAttemptCount()).isZero();
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.getLastError()).isNull();
    }

    @Test
    @DisplayName("발송 대기 내역을 claim하면 처리 중 상태가 되고 시도 횟수가 증가한다")
    void claimDelivery() {
        NotificationDelivery delivery = createDelivery();

        delivery.claim(CLAIMED_AT);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PROCESSING);
        assertThat(delivery.getClaimedAt()).isEqualTo(CLAIMED_AT);
        assertThat(delivery.getAttemptCount()).isOne();
    }

    @Test
    @DisplayName("처리 중인 내역을 성공 처리하면 발송 완료 상태가 된다")
    void markDeliveryAsSent() {
        NotificationDelivery delivery = createClaimedDelivery();

        delivery.markSent(SENT_AT);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(delivery.getSentAt()).isEqualTo(SENT_AT);
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.getLastError()).isNull();
    }

    @Test
    @DisplayName("처리 중인 내역을 재시도 대기 상태로 변경하면 재시도 시간이 저장된다")
    void markDeliveryAsRetryWait() {
        NotificationDelivery delivery = createClaimedDelivery();

        delivery.markRetry(NEXT_RETRY_AT, "일시적인 발송 실패");

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RETRY_WAIT);
        assertThat(delivery.getNextRetryAt()).isEqualTo(NEXT_RETRY_AT);
        assertThat(delivery.getLastError()).isEqualTo("일시적인 발송 실패");

        delivery.claim(SENT_AT);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PROCESSING);
        assertThat(delivery.getClaimedAt()).isEqualTo(SENT_AT);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("처리 중인 내역을 실패 처리하면 실패 상태가 된다")
    void markDeliveryAsFailed() {
        NotificationDelivery delivery = createClaimedDelivery();

        delivery.markFailed("영구적인 발송 실패");

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.getLastError()).isEqualTo("영구적인 발송 실패");
    }

    @Test
    @DisplayName("시간 정보가 없으면 상태를 변경할 수 없다")
    void rejectMissingTime() {
        NotificationDelivery delivery = createDelivery();

        assertInvalidTime(() -> delivery.claim(null));

        delivery.claim(CLAIMED_AT);
        assertInvalidTime(() -> delivery.markSent(null));
        assertInvalidTime(() -> delivery.markRetry(null, "일시적인 발송 실패"));
    }

    @Test
    @DisplayName("현재 상태에 허용되지 않은 상태 전이는 수행할 수 없다")
    void rejectInvalidStatusTransition() {
        NotificationDelivery delivery = createDelivery();

        assertInvalidStatus(() -> delivery.markSent(SENT_AT));
        assertInvalidStatus(() -> delivery.markRetry(NEXT_RETRY_AT, "일시적인 발송 실패"));
        assertInvalidStatus(() -> delivery.markFailed("영구적인 발송 실패"));

        delivery.claim(CLAIMED_AT);
        delivery.markSent(SENT_AT);

        assertInvalidStatus(() -> delivery.claim(SENT_AT));
        assertInvalidStatus(() -> delivery.markSent(SENT_AT));
    }

    private NotificationDelivery createDelivery() {
        return NotificationDelivery.create(mock(Notification.class), mock(WebPushSubscription.class));
    }

    private NotificationDelivery createClaimedDelivery() {
        NotificationDelivery delivery = createDelivery();
        delivery.claim(CLAIMED_AT);
        return delivery;
    }

    private void assertInvalidTime(ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_DELIVERY_TIME);
    }

    private void assertInvalidStatus(ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_DELIVERY_UPDATE_STATUS);
    }
}
