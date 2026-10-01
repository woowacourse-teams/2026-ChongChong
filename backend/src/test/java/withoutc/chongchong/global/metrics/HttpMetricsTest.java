package withoutc.chongchong.global.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HttpMetricsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 실제_API_요청을_Prometheus_요청_수와_응답_시간_버킷에_반영한다() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk());

        String metrics = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> samples = metrics.lines().toList();

        assertThat(samples).anyMatch(line -> line.startsWith("http_server_requests_seconds_count{")
                && line.contains("uri=\"/api/auth/csrf\"")
                && line.contains("status=\"200\"")
                && line.matches(".* [1-9][0-9]*(\\.[0-9]+)?"));
        assertThat(samples).anyMatch(line -> line.startsWith("http_server_requests_seconds_bucket{")
                && line.contains("uri=\"/api/auth/csrf\"")
                && line.contains("le=\"+Inf\""));
    }
}
