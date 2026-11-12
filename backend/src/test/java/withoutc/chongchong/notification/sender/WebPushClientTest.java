package withoutc.chongchong.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notification.config.WebPushVapidProperties;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;

class WebPushClientTest {

    private static final String P256DH = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ECNamedCurveTable.getParameterSpec("prime256v1").getG().getEncoded(false));
    private static final String AUTH = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
    private static final WebPushVapidProperties VAPID = new WebPushVapidProperties(
            "BDcLQBzrmGYpdNmsaQrk9_uU9Q_wQb2ziEN5AIcoUq4bi2uKlC2UKcmfBuUdJj_NzcDu-ldeUpif_na9i-u3HKw",
            "67yyNB109eqWbw7r3HcJCuwmLDaDROaI6DJH4nCWUYU",
            "mailto:test@example.com"
    );

    private HttpServer server;
    private String endpoint;
    private final List<ReceivedRequest> receivedRequests = new CopyOnWriteArrayList<>();
    private final Set<Integer> remotePorts = ConcurrentHashMap.newKeySet();
    private volatile CountDownLatch holdResponse = new CountDownLatch(0);
    private volatile int responseStatus = 201;
    private volatile String responseBody = "";

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/push", this::handle);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/push/subscription";
    }

    @AfterEach
    void stopServer() {
        holdResponse.countDown();
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        receivedRequests.add(new ReceivedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestHeaders().getFirst("Urgency"),
                exchange.getRequestHeaders().getFirst("TTL"),
                exchange.getRequestHeaders().getFirst("Content-Encoding"),
                exchange.getRequestHeaders().getFirst("Authorization"),
                requestBody.length
        ));
        remotePorts.add(exchange.getRemoteAddress().getPort());
        try {
            holdResponse.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(responseStatus, body.length == 0 ? -1 : body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    @Test
    @DisplayName("암호화한 페이로드를 긴급도 high와 VAPID 인증 헤더를 붙여 POST로 보낸다")
    void sendsEncryptedRequestWithHeaders() {
        try (WebPushClient client = new WebPushClient(VAPID, Duration.ofSeconds(2))) {
            responseBody = "created";

            WebPushClient.WebPushResponse response = client.send(endpoint, P256DH, AUTH, "payload");

            assertThat(response.status()).isEqualTo(201);
            assertThat(response.responseBody()).isEqualTo("created");
            assertThat(receivedRequests).singleElement().satisfies(request -> {
                assertThat(request.method()).isEqualTo("POST");
                assertThat(request.urgency()).isEqualTo("high");
                assertThat(request.ttl()).isNotBlank();
                assertThat(request.contentEncoding()).isEqualTo("aes128gcm");
                assertThat(request.authorization()).startsWith("vapid t=");
                assertThat(request.bodyLength()).isGreaterThan("payload".length());
            });
        }
    }

    @Test
    @DisplayName("여러 번 보내도 같은 연결을 재사용한다")
    void reusesConnection() {
        try (WebPushClient client = new WebPushClient(VAPID, Duration.ofSeconds(2))) {
            for (int i = 0; i < 3; i++) {
                client.send(endpoint, P256DH, AUTH, "payload-" + i);
            }

            assertThat(receivedRequests).hasSize(3);
            assertThat(remotePorts).hasSize(1);
        }
    }

    @Test
    @DisplayName("Push 서비스의 오류 응답은 상태 코드를 그대로 돌려주고, 긴 응답 본문은 2,000자로 자른다")
    void returnsErrorStatusWithTruncatedBody() {
        try (WebPushClient client = new WebPushClient(VAPID, Duration.ofSeconds(2))) {
            responseStatus = 410;
            responseBody = "x".repeat(3_000);

            WebPushClient.WebPushResponse response = client.send(endpoint, P256DH, AUTH, "payload");

            assertThat(response.status()).isEqualTo(410);
            assertThat(response.responseBody()).hasSize(2_000);
        }
    }

    @Test
    @DisplayName("발송 제한 시간이 지나면 요청을 포기하고 재시도 가능한 통신 오류를 반환한다")
    void failsAfterSendTimeout() {
        holdResponse = new CountDownLatch(1);
        try (WebPushClient client = new WebPushClient(VAPID, Duration.ofMillis(200))) {
            long startedAt = System.nanoTime();

            assertThatThrownBy(() -> client.send(endpoint, P256DH, AUTH, "payload"))
                    .isInstanceOf(WebPushException.class)
                    .satisfies(exception -> assertThat(((WebPushException) exception).getErrorCode())
                            .isEqualTo(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED));

            assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(2));
            holdResponse.countDown();
        }
    }

    @Test
    @DisplayName("연결할 수 없는 Push 서비스는 재시도 가능한 통신 오류를 반환한다")
    void failsWhenConnectionRefused() {
        server.stop(0);
        try (WebPushClient client = new WebPushClient(VAPID, Duration.ofSeconds(1))) {
            assertThatThrownBy(() -> client.send(endpoint, P256DH, AUTH, "payload"))
                    .isInstanceOf(WebPushException.class)
                    .satisfies(exception -> assertThat(((WebPushException) exception).getErrorCode())
                            .isEqualTo(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED));
        }
    }

    @Test
    @DisplayName("닫을 때 이미 보낸 요청의 응답을 기다리고, 닫은 뒤의 발송은 통신 오류로 처리한다")
    void closeWaitsForInFlightRequest() throws Exception {
        holdResponse = new CountDownLatch(1);
        WebPushClient client = new WebPushClient(VAPID, Duration.ofSeconds(3));
        CountDownLatch sent = new CountDownLatch(1);
        Thread sender = Thread.ofVirtual().start(() -> {
            WebPushClient.WebPushResponse response = client.send(endpoint, P256DH, AUTH, "payload");
            if (response.status() == 201) {
                sent.countDown();
            }
        });
        waitUntilReceived(1);

        Thread closer = Thread.ofVirtual().start(client::close);
        closer.join(200);
        assertThat(closer.isAlive()).isTrue();

        holdResponse.countDown();
        closer.join(3_000);
        sender.join(3_000);

        assertThat(closer.isAlive()).isFalse();
        assertThat(sent.getCount()).isZero();
        assertThatThrownBy(() -> client.send(endpoint, P256DH, AUTH, "payload"))
                .isInstanceOf(WebPushException.class)
                .satisfies(exception -> assertThat(((WebPushException) exception).getErrorCode())
                        .isEqualTo(WebPushErrorCode.WEB_PUSH_TRANSPORT_FAILED));
    }

    private void waitUntilReceived(int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (receivedRequests.size() < count && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(receivedRequests).hasSize(count);
    }

    private record ReceivedRequest(
            String method,
            String urgency,
            String ttl,
            String contentEncoding,
            String authorization,
            int bodyLength
    ) {
    }
}
