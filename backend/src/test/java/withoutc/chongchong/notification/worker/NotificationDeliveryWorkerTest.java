package withoutc.chongchong.notification.worker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryWorkerTest {

    private static final int BATCH_SIZE = 100;
    private static final Long DELIVERY_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 2L;

    @Mock
    private DeliveryClaimService deliveryClaimService;

    @Mock
    private DeliveryRecoveryService deliveryRecoveryService;

    @Mock
    private NotificationSender sender;

    @Mock
    private DeliveryResultService deliveryResultService;

    private NotificationDeliveryWorker worker;
    private ClaimedDelivery delivery;

    @BeforeEach
    void setUp() {
        worker = new NotificationDeliveryWorker(
                deliveryClaimService,
                deliveryRecoveryService,
                sender,
                deliveryResultService
        );
        delivery = new ClaimedDelivery(
                DELIVERY_ID,
                SUBSCRIPTION_ID,
                "https://push.example.com/subscription",
                "p256dh",
                "auth",
                3L,
                "제목",
                "본문",
                "/studies/1/notices/3"
        );
    }

    @Test
    @DisplayName("전송 성공 시 SENT로 처리한다")
    void markSentWhenSendingSucceeds() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.SENT);

        worker.processOne(delivery);

        verify(deliveryResultService).markSent(DELIVERY_ID);
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("재시도 가능한 전송 실패 시 RETRY_WAIT으로 처리한다")
    void markRetryWhenSendingFailsRetryably() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.RETRYABLE_FAILURE);

        worker.processOne(delivery);

        verify(deliveryResultService).markRetry(
                DELIVERY_ID,
                WebPushSendResult.RETRYABLE_FAILURE.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("만료된 구독 응답이면 Delivery 실패와 구독 비활성화를 처리한다")
    void markExpiredWhenSubscriptionIsExpired() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.SUBSCRIPTION_EXPIRED);

        worker.processOne(delivery);

        verify(deliveryResultService).markExpired(
                DELIVERY_ID,
                SUBSCRIPTION_ID,
                WebPushSendResult.SUBSCRIPTION_EXPIRED.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("영구적인 전송 실패 시 FAILED로 처리한다")
    void markFailedWhenSendingFailsPermanently() {
        when(sender.send(delivery)).thenReturn(WebPushSendResult.PERMANENT_FAILURE);

        worker.processOne(delivery);

        verify(deliveryResultService).markFailed(
                DELIVERY_ID,
                WebPushSendResult.PERMANENT_FAILURE.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("전송 통신 예외는 재시도 처리하고 다음 Delivery를 진행한다")
    void markRetryWhenTransportFails() {
        WebPushException exception = new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        when(sender.send(delivery)).thenThrow(exception);

        worker.processOne(delivery);

        verify(deliveryResultService).markRetry(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("중단 예외는 현재 Delivery를 재시도 처리한 뒤 다시 던진다")
    void rethrowWhenSendingIsInterrupted() {
        WebPushException exception = new WebPushException(WebPushErrorCode.WEB_PUSH_INTERRUPTED);
        when(sender.send(delivery)).thenThrow(exception);

        assertThatThrownBy(() -> worker.processOne(delivery))
                .isSameAs(exception);

        verify(deliveryResultService).markRetry(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_INTERRUPTED.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("그 외 Web Push 예외는 영구 실패로 처리한다")
    void markFailedWhenWebPushExceptionIsNotRetryable() {
        WebPushException exception = new WebPushException(WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED);
        when(sender.send(delivery)).thenThrow(exception);

        worker.processOne(delivery);

        verify(deliveryResultService).markFailed(
                DELIVERY_ID,
                WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED.getMessage()
        );
        verifyNoMoreInteractions(deliveryResultService);
    }

    @Test
    @DisplayName("배치 처리 시 고착 Delivery 복구 후 claim한 Delivery를 전송한다")
    void processBatch() {
        when(deliveryClaimService.claimBatch(BATCH_SIZE)).thenReturn(List.of(delivery));
        when(sender.send(delivery)).thenReturn(WebPushSendResult.SENT);

        worker.process();

        InOrder inOrder = inOrder(deliveryRecoveryService, deliveryClaimService, sender, deliveryResultService);
        inOrder.verify(deliveryRecoveryService).recover(BATCH_SIZE);
        inOrder.verify(deliveryClaimService).claimBatch(BATCH_SIZE);
        inOrder.verify(sender).send(delivery);
        inOrder.verify(deliveryResultService).markSent(DELIVERY_ID);
    }
}
