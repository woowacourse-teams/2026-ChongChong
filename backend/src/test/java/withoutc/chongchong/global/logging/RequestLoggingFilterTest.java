package withoutc.chongchong.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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

    @Test
    void 스터디_목록의_성공_로그에는_빈_목록도_건수를_남긴다() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest success = new MockHttpServletRequest("GET", "/api/studies/me");
            filter.doFilter(success, new MockHttpServletResponse(),
                    (request, response) -> RequestLoggingContext.recordStudyCount(success, 0));

            MockHttpServletRequest failure = new MockHttpServletRequest("GET", "/api/studies/me");
            filter.doFilter(failure, new MockHttpServletResponse(), (request, response) -> {
                RequestLoggingContext.recordStudyCount(failure, 2);
                ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            });

            MockHttpServletRequest interrupted = new MockHttpServletRequest("GET", "/api/studies/me");
            assertThatThrownBy(() -> filter.doFilter(interrupted, new MockHttpServletResponse(), (request, response) -> {
                RequestLoggingContext.recordStudyCount(interrupted, 2);
                throw new ServletException("response write failed");
            })).isInstanceOf(ServletException.class);

            MockHttpServletRequest other = new MockHttpServletRequest("GET", "/api/studies/public");
            filter.doFilter(other, new MockHttpServletResponse(), (request, response) -> {});

            assertThat(appender.list).hasSize(4);
            assertThat(appender.list.get(0).getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("study_count");
                assertThat(pair.value).isEqualTo(0);
            });
            assertThat(appender.list.subList(1, appender.list.size())).allSatisfy(event ->
                    assertThat(event.getKeyValuePairs()).noneMatch(pair -> pair.key.equals("study_count")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
