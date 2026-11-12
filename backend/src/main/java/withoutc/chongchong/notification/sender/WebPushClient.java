package withoutc.chongchong.notification.sender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Urgency;
import org.jose4j.lang.JoseException;
import withoutc.chongchong.notification.config.WebPushVapidProperties;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

// 애플리케이션 전체에서 HTTP 클라이언트 하나를 같이 써서 Push 서비스와의 연결을 재사용한다.
// 동시에 나가는 요청 수는 발송 스레드 수(web-push.delivery.concurrency)를 넘지 않는다. 각 스레드가 응답을 기다리기 때문이다.
public class WebPushClient implements AutoCloseable {

    private static final int MAX_RESPONSE_BODY_LENGTH = 2_000;

    private final WebPushRequestFactory requestFactory;
    private final HttpClient httpClient;
    private final Duration sendTimeout;

    public WebPushClient(WebPushVapidProperties vapidProperties, Duration sendTimeout) {
        this(createRequestFactory(vapidProperties), createHttpClient(sendTimeout), sendTimeout);
    }

    WebPushClient(WebPushRequestFactory requestFactory, HttpClient httpClient, Duration sendTimeout) {
        validateSendTimeout(sendTimeout);
        this.requestFactory = requestFactory;
        this.httpClient = httpClient;
        this.sendTimeout = sendTimeout;
    }

    private static WebPushRequestFactory createRequestFactory(WebPushVapidProperties vapidProperties) {
        try {
            return new WebPushRequestFactory(
                    vapidProperties.publicKey(),
                    vapidProperties.privateKey(),
                    vapidProperties.subject()
            );
        } catch (GeneralSecurityException exception) {
            throw new WebPushException(WebPushErrorCode.INVALID_WEB_PUSH_CONFIG);
        }
    }

    private static HttpClient createHttpClient(Duration sendTimeout) {
        validateSendTimeout(sendTimeout);
        return HttpClient.newBuilder()
                .connectTimeout(sendTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public WebPushResponse send(String endpoint, String p256dh, String auth, String payload) {
        // 암호화와 서명은 제한 시간을 재기 전에 끝낸다. 제한 시간은 전송과 응답 대기에만 쓴다.
        HttpRequest request = createRequest(endpoint, p256dh, auth, payload);
        Future<HttpResponse<String>> responseFuture = null;
        try {
            responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = responseFuture.get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return new WebPushResponse(response.statusCode(), truncate(response.body()));
        } catch (ExecutionException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        } catch (TimeoutException exception) {
            cancel(responseFuture);
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED);
        } catch (InterruptedException exception) {
            cancel(responseFuture);
            Thread.currentThread().interrupt();
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_INTERRUPTED);
        }
    }

    private HttpRequest createRequest(String endpoint, String p256dh, String auth, String payload) {
        try {
            Notification notification = new Notification(endpoint, p256dh, auth, payload, Urgency.HIGH);
            nl.martijndwars.webpush.HttpRequest prepared = requestFactory.create(notification);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(prepared.getUrl()))
                    .timeout(sendTimeout)
                    .POST(body(prepared.getBody()));
            prepared.getHeaders().forEach(builder::header);
            return builder.build();
        } catch (JoseException | GeneralSecurityException | IOException exception) {
            throw new WebPushException(WebPushErrorCode.WEB_PUSH_CRYPTO_FAILED);
        }
    }

    private HttpRequest.BodyPublisher body(byte[] body) {
        if (body == null) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private static void validateSendTimeout(Duration sendTimeout) {
        if (sendTimeout == null || sendTimeout.isNegative() || sendTimeout.isZero()
                || sendTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("푸시 발송 제한 시간은 1밀리초 이상이어야 합니다.");
        }
    }

    private void cancel(Future<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private String truncate(String responseBody) {
        if (responseBody == null) {
            return "";
        }
        if (responseBody.length() <= MAX_RESPONSE_BODY_LENGTH) {
            return responseBody;
        }
        return responseBody.substring(0, MAX_RESPONSE_BODY_LENGTH);
    }

    // 새 요청은 받지 않고, 이미 보낸 요청은 제한 시간만큼 응답을 기다린 뒤 연결을 닫는다.
    @Override
    public void close() {
        httpClient.shutdown();
        try {
            if (!httpClient.awaitTermination(sendTimeout)) {
                httpClient.shutdownNow();
            }
        } catch (InterruptedException exception) {
            httpClient.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public record WebPushResponse(int status, String responseBody) {
    }
}
