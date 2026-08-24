package withoutc.chongchong.auth.social.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import withoutc.chongchong.auth.config.KakaoLoginProperties;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.social.kakao.dto.KakaoTokenResponse;

class KakaoTokenClientTest {

    private static final String AUTHORIZATION_CODE = "sensitive-authorization-code";
    private static final String REST_API_KEY = "sensitive-rest-api-key";
    private static final String CLIENT_SECRET = "sensitive-client-secret";
    private static final String ACCESS_TOKEN = "sensitive-kakao-access-token";

    private final AtomicReference<StubResponse> stubResponse = new AtomicReference<>();
    private final AtomicReference<RecordedRequest> recordedRequest = new AtomicReference<>();

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/oauth/token", this::handleTokenRequest);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Authorization Code와 서버 설정을 Form으로 보내 Kakao Access Token을 받는다")
    void exchangeAuthorizationCode() {
        respond(200, """
                {
                  "token_type": "bearer",
                  "access_token": "%s",
                  "expires_in": 43199,
                  "refresh_token": "unused-provider-refresh-token"
                }
                """.formatted(ACCESS_TOKEN));

        KakaoAccessToken accessToken = tokenClient(Duration.ofSeconds(1)).exchange(AUTHORIZATION_CODE);

        assertThat(accessToken.value()).isEqualTo(ACCESS_TOKEN);
        assertTokenRequest(recordedRequest.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 500})
    @DisplayName("Kakao Token Endpoint 오류를 공통 소셜 인증 실패로 변환한다")
    void convertKakaoHttpError(int status) {
        respond(status, """
                {
                  "error": "invalid_grant",
                  "error_description": "sensitive-provider-error"
                }
                """);

        assertAuthenticationFailed(() -> tokenClient(Duration.ofSeconds(1)).exchange(AUTHORIZATION_CODE));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "{broken-json",
            "{}",
            "{\"access_token\":\" \"}",
            "{\"access_token\":\"token\\r\\nvalue\"}"
    })
    @DisplayName("읽을 수 없거나 Header에 사용할 수 없는 Kakao Token 응답을 공통 소셜 인증 실패로 변환한다")
    void rejectInvalidResponse(String responseBody) {
        respond(200, responseBody);

        assertAuthenticationFailed(() -> tokenClient(Duration.ofSeconds(1)).exchange(AUTHORIZATION_CODE));
    }

    @Test
    @DisplayName("Kakao Token Endpoint 응답 제한 시간을 넘으면 공통 소셜 인증 실패로 변환한다")
    void rejectReadTimeout() {
        stubResponse.set(new StubResponse(200, "{\"access_token\":\"late-token\"}", Duration.ofMillis(300)));

        assertAuthenticationFailed(() -> tokenClient(Duration.ofMillis(50)).exchange(AUTHORIZATION_CODE));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("비어 있는 Authorization Code를 외부 호출 없이 거부한다")
    void rejectBlankAuthorizationCode(String authorizationCode) {
        assertAuthenticationFailed(() -> tokenClient(Duration.ofSeconds(1)).exchange(authorizationCode));

        assertThat(recordedRequest.get()).isNull();
    }

    @Test
    @DisplayName("Kakao Token 객체의 문자열 표현에 Token 원문을 노출하지 않는다")
    void redactTokenFromString() {
        KakaoAccessToken accessToken = new KakaoAccessToken(ACCESS_TOKEN);
        KakaoTokenResponse response = new KakaoTokenResponse(ACCESS_TOKEN);

        assertThat(accessToken.toString())
                .contains("value=REDACTED")
                .doesNotContain(ACCESS_TOKEN);
        assertThat(response.toString())
                .contains("accessToken=REDACTED")
                .doesNotContain(ACCESS_TOKEN);
    }

    private KakaoTokenClient tokenClient(Duration readTimeout) {
        KakaoLoginProperties properties = new KakaoLoginProperties(
                REST_API_KEY,
                CLIENT_SECRET,
                URI.create("http://localhost:3005/auth/kakao/callback"),
                URI.create("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
                        + "/oauth/token"),
                URI.create("https://kapi.kakao.com/v2/user/me"),
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
        return new KakaoTokenClient(restClient, properties);
    }

    private void respond(int status, String body) {
        stubResponse.set(new StubResponse(status, body, Duration.ZERO));
    }

    private void handleTokenRequest(HttpExchange exchange) throws IOException {
        recordedRequest.set(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
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

    private void assertTokenRequest(RecordedRequest request) {
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.contentType()).startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        assertThat(parseForm(request.body())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "grant_type", "authorization_code",
                "client_id", REST_API_KEY,
                "redirect_uri", "http://localhost:3005/auth/kakao/callback",
                "code", AUTHORIZATION_CODE,
                "client_secret", CLIENT_SECRET
        ));
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        Arrays.stream(body.split("&"))
                .map(field -> field.split("=", 2))
                .forEach(field -> values.put(decode(field[0]), decode(field[1])));
        return values;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void assertAuthenticationFailed(ThrowingCall call) {
        Throwable exception = catchThrowable(call::execute);

        assertThat(exception)
                .isInstanceOf(AuthException.class)
                .extracting(thrown -> ((AuthException) thrown).getErrorCode())
                .isEqualTo(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);

        assertThat(exception)
                .hasMessage(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED.getMessage());
        assertThat(exception.getMessage())
                .doesNotContain(
                        AUTHORIZATION_CODE,
                        REST_API_KEY,
                        CLIENT_SECRET,
                        ACCESS_TOKEN,
                        "sensitive-provider-error"
                );
    }

    @FunctionalInterface
    private interface ThrowingCall {

        void execute();
    }

    private record StubResponse(int status, String body, Duration delay) {
    }

    private record RecordedRequest(String method, String contentType, String body) {
    }
}
