package withoutc.chongchong.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Security;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

class WebPushClientTest {

    private static final String ENDPOINT = "https://push.example.com/subscription";
    private static final String P256DH = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ECNamedCurveTable.getParameterSpec("prime256v1").getG().getEncoded(false));
    private static final String AUTH = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("발송 요청에 긴급도 high를 지정한다")
    void sendsNotificationWithHighUrgency() throws Exception {
        PushService pushService = mock(PushService.class);
        HttpResponse response = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(pushService.sendAsync(any(Notification.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(201);
        WebPushClient client = new WebPushClient(pushService, Duration.ofSeconds(1));

        WebPushClient.WebPushResponse result = client.send(ENDPOINT, P256DH, AUTH, "payload");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(pushService).sendAsync(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getUrgency()).isEqualTo(Urgency.HIGH);
        assertThat(result.status()).isEqualTo(201);
    }

    @Test
    @DisplayName("발송 제한 시간이 지나면 HTTP 요청을 취소하고 재시도 가능한 통신 오류를 반환한다")
    void cancelsRequestAfterSendTimeout() throws Exception {
        PushService pushService = mock(PushService.class);
        CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();
        when(pushService.sendAsync(any(Notification.class))).thenReturn(responseFuture);
        WebPushClient client = new WebPushClient(pushService, Duration.ofMillis(50));

        assertThatThrownBy(() -> client.send(ENDPOINT, P256DH, AUTH, "payload"))
                .isInstanceOf(WebPushException.class)
                .satisfies(exception -> assertThat(((WebPushException) exception).getErrorCode())
                        .isEqualTo(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED));

        assertThat(responseFuture.isCancelled()).isTrue();
        verify(pushService).sendAsync(any(Notification.class));
    }
}
