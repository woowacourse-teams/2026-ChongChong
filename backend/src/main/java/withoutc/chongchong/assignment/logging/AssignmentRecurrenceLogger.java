package withoutc.chongchong.assignment.logging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 반복 과제 워커의 로그 이벤트와 실패 메트릭을 한곳에 모은다. CloudWatch 검색에 쓰는 공통 필드를 항상 붙인다.
@Slf4j
@Component
public class AssignmentRecurrenceLogger {

    private static final String COMPONENT = "assignment_recurrence_worker";

    private final String service;
    private final String environment;
    private final Counter generationFailures;

    public AssignmentRecurrenceLogger(
            @Value("${spring.application.name:unknown}") String service,
            @Value("${app.environment:local}") String environment,
            MeterRegistry meterRegistry
    ) {
        this.service = service;
        this.environment = environment;
        this.generationFailures = Counter.builder("assignment.recurrence.generation.failed")
                .description("반복 과제 회차 생성에 실패한 횟수")
                .register(meterRegistry);
    }

    public void occurrenceCreated(Long recurrenceId, Long assignmentId, LocalDate occurrenceDate) {
        event(Level.INFO, "assignment_recurrence_occurrence_created", recurrenceId)
                .addKeyValue("assignment_id", assignmentId)
                .addKeyValue("occurrence_date", occurrenceDate)
                .log("assignment recurrence occurrence created");
    }

    public void pastOccurrencesSkipped(Long recurrenceId, int skipped) {
        event(Level.WARN, "assignment_recurrence_occurrences_skipped", recurrenceId)
                .addKeyValue("skipped", skipped)
                .log("past assignment recurrence occurrences skipped");
    }

    public void noSubmitters(Long recurrenceId) {
        event(Level.INFO, "assignment_recurrence_no_submitters", recurrenceId)
                .log("assignment recurrence occurrence skipped without submitters");
    }

    public void occurrenceExists(Long recurrenceId, Throwable throwable) {
        event(Level.WARN, "assignment_recurrence_occurrence_exists", recurrenceId)
                .log("assignment recurrence occurrence already exists", throwable);
    }

    public void generationFailed(Long recurrenceId, Throwable throwable) {
        generationFailures.increment();
        event(Level.ERROR, "assignment_recurrence_generation_failed", recurrenceId)
                .log("assignment recurrence generation failed", throwable);
    }

    private LoggingEventBuilder event(Level level, String event, Long recurrenceId) {
        return log.atLevel(level)
                .addKeyValue("service", service)
                .addKeyValue("environment", environment)
                .addKeyValue("component", COMPONENT)
                .addKeyValue("event", event)
                .addKeyValue("recurrence_id", recurrenceId);
    }
}
