package withoutc.chongchong.auth.social.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import withoutc.chongchong.auth.config.KakaoLoginProperties;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse;

class KakaoUserInfoClientTest {

    private static final String ACCESS_TOKEN = "sensitive-kakao-access-token";

    private final AtomicReference<StubResponse> stubResponse = new AtomicReference<>();
    private final AtomicReference<RecordedRequest> recordedRequest = new AtomicReference<>();

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v2/user/me", this::handleUserInfoRequest);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Kakao Access Token으로 HTTPS 프로필을 요청하고 사용자 정보를 받는다")
    void fetchUserInfo() {
        respond(200, """
                {
                  "id": 123456789,
                  "kakao_account": {
                    "profile": {
                      "nickname": "총총이",
                      "profile_image_url": "https://example.com/profile.png"
                    }
                  }
                }
                """);

        KakaoUserInfoResponse response = userInfoClient(Duration.ofSeconds(1))
                .fetch(new KakaoAccessToken(ACCESS_TOKEN));

        assertThat(response.id()).isEqualTo(123456789L);
        assertThat(response.kakaoAccount().profile().nickname()).isEqualTo("총총이");
        assertThat(response.kakaoAccount().profile().profileImageUrl())
                .isEqualTo("https://example.com/profile.png");
        assertUserInfoRequest(recordedRequest.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 500})
    @DisplayName("Kakao 사용자 정보 Endpoint 오류를 공통 소셜 인증 실패로 변환한다")
    void convertKakaoHttpError(int status) {
        respond(status, "{\"msg\":\"sensitive-provider-error\",\"code\":-401}");

        assertAuthenticationFailed(() -> userInfoClient(Duration.ofSeconds(1))
                .fetch(new KakaoAccessToken(ACCESS_TOKEN)));
    }

    @Test
    @DisplayName("읽을 수 없는 Kakao 사용자 정보 응답을 공통 소셜 인증 실패로 변환한다")
    void rejectUnreadableResponse() {
        respond(200, "{broken-json");

        assertAuthenticationFailed(() -> userInfoClient(Duration.ofSeconds(1))
                .fetch(new KakaoAccessToken(ACCESS_TOKEN)));
    }

    @Test
    @DisplayName("Kakao 사용자 정보 응답 제한 시간을 넘으면 공통 소셜 인증 실패로 변환한다")
    void rejectReadTimeout() {
        stubResponse.set(new StubResponse(200, "{\"id\":123}", Duration.ofMillis(300)));

        assertAuthenticationFailed(() -> userInfoClient(Duration.ofMillis(50))
                .fetch(new KakaoAccessToken(ACCESS_TOKEN)));
    }

    @Test
    @DisplayName("Kakao Access Token이 없으면 외부 호출 없이 거부한다")
    void rejectMissingAccessToken() {
        assertAuthenticationFailed(() -> userInfoClient(Duration.ofSeconds(1)).fetch(null));

        assertThat(recordedRequest.get()).isNull();
    }

    private KakaoUserInfoClient userInfoClient(Duration readTimeout) {
        URI userInfoUri = URI.create("http://" + server.getAddress().getHostString() + ":"
                + server.getAddress().getPort() + "/v2/user/me");
        KakaoLoginProperties properties = new KakaoLoginProperties(
                "test-rest-api-key",
                "test-client-secret",
                URI.create("http://localhost:3005/auth/kakao/callback"),
                URI.create("https://kauth.kakao.com/oauth/token"),
                userInfoUri,
                Duration.ofSeconds(1),
                readTimeout
        );
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        return new KakaoUserInfoClient(restClient, properties);
    }

    private void respond(int status, String body) {
        stubResponse.set(new StubResponse(status, body, Duration.ZERO));
    }

    private void handleUserInfoRequest(HttpExchange exchange) throws IOException {
        recordedRequest.set(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders().getFirst("Authorization")
        ));

        StubResponse response = stubResponse.get();
        waitBeforeResponse(response.delay());
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(response.status(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void waitBeforeResponse(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void assertUserInfoRequest(RecordedRequest request) {
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.query()).isEqualTo("secure_resource=true");
        assertThat(request.authorization()).isEqualTo("Bearer " + ACCESS_TOKEN);
    }

    private void assertAuthenticationFailed(ThrowingCall call) {
        Throwable exception = catchThrowable(call::execute);

        assertThat(exception)
                .isInstanceOf(AuthException.class)
                .extracting(thrown -> ((AuthException) thrown).getErrorCode())
                .isEqualTo(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
        assertThat(exception).hasMessage(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED.getMessage());
        assertThat(exception.getMessage()).doesNotContain(ACCESS_TOKEN, "sensitive-provider-error");
    }

    @FunctionalInterface
    private interface ThrowingCall {

        void execute();
    }

    private record StubResponse(int status, String body, Duration delay) {
    }

    private record RecordedRequest(String method, String query, String authorization) {
    }
}
