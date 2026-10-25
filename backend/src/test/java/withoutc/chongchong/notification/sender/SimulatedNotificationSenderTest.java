package withoutc.chongchong.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

class SimulatedNotificationSenderTest {

    private static final ClaimedDelivery DELIVERY = new ClaimedDelivery(
            1L, 2L, "https://load-test.invalid/subscriptions/2", "synthetic-p256dh", "synthetic-auth",
            3L, "부하 테스트", "테스트 알림", "/studies/1/notices/3", LocalDateTime.of(2026, 10, 15, 10, 0)
    );

    @Test
    @DisplayName("실제 Push 구독이 아닌 데이터로도 외부 요청 없이 성공 결과를 반환한다")
    void returnSentWithoutRealSubscription() {
        SimulatedNotificationSender sender = new SimulatedNotificationSender(0);

        assertThat(sender.send(DELIVERY)).isEqualTo(WebPushSendResult.SENT);
    }

    @Test
    @Timeout(2)
    @DisplayName("설정한 지연 시간이 지난 뒤 성공 결과를 반환한다")
    void waitBeforeReturningSent() {
        long delayMs = 40;
        SimulatedNotificationSender sender = new SimulatedNotificationSender(delayMs);
        long startedAt = System.nanoTime();

        WebPushSendResult result = sender.send(DELIVERY);
        long elapsedNanos = System.nanoTime() - startedAt;

        assertThat(result).isEqualTo(WebPushSendResult.SENT);
        assertThat(elapsedNanos).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(delayMs));
    }

    @Test
    @DisplayName("대기 중단은 인터럽트 상태를 보존하고 worker가 처리하는 중단 예외로 전달한다")
    void preserveInterruption() {
        SimulatedNotificationSender sender = new SimulatedNotificationSender(500);
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> sender.send(DELIVERY))
                    .isInstanceOfSatisfying(WebPushException.class,
                            exception -> assertThat(exception.getErrorCode()).isEqualTo(WebPushErrorCode.WEB_PUSH_INTERRUPTED));
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
