package withoutc.chongchong.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.health.probes.enabled=true"
)
@ActiveProfiles("test")
class ReadinessSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("배포 준비 상태를 인증 없이 조회하고 내부 상세 정보는 노출하지 않는다")
    void allowReadinessWithoutAuthentication() throws Exception {
        HttpResponse<String> response = get("/actuator/health/readiness");

        assertThat(response.statusCode()).isEqualTo(200);
        var body = objectMapper.readTree(response.body());
        assertThat(body.path("status").stringValue()).isEqualTo("UP");
        assertThat(body.has("components")).isFalse();
    }

    @Test
    @DisplayName("요청을 받을 준비가 되지 않으면 인증 오류 대신 503을 반환한다")
    void reportUnavailableWhenRefusingTraffic() throws Exception {
        AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC);
        try {
            HttpResponse<String> response = get("/actuator/health/readiness");

            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(objectMapper.readTree(response.body()).path("status").stringValue())
                    .isEqualTo("OUT_OF_SERVICE");
        } finally {
            AvailabilityChangeEvent.publish(applicationContext, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health/liveness", "/actuator/env", "/api/users/me"})
    @DisplayName("readiness 허용으로 다른 보호 경로의 인증이 해제되지 않는다")
    void keepOtherEndpointsProtected(String path) throws Exception {
        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(objectMapper.readTree(response.body()).path("code").stringValue())
                .isEqualTo("AUTHENTICATION_REQUIRED");
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
