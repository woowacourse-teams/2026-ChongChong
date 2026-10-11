package withoutc.chongchong.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void 매핑되지_않은_요청은_원본_URI를_로그에_사용하지_않는다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/token-value");

        assertThat(filter.resolveRoute(request)).isEqualTo("UNMATCHED_ROUTE");
    }

    @Test
    void 매핑된_요청은_패턴을_로그에_사용한다() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/studies/1");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/api/studies/{studyId}"
        );

        assertThat(filter.resolveRoute(request)).isEqualTo("/api/studies/{studyId}");
    }
}
