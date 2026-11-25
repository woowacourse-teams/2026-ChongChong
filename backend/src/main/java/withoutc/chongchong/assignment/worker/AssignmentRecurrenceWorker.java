package withoutc.chongchong.assignment.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.config.AssignmentRecurrenceProperties;
import withoutc.chongchong.assignment.logging.AssignmentRecurrenceLogger;
import withoutc.chongchong.assignment.repository.projection.AssignmentRecurrenceCandidate;
import withoutc.chongchong.assignment.service.AssignmentRecurrenceGenerationService;

@Component
@RequiredArgsConstructor
public class AssignmentRecurrenceWorker implements ApplicationListener<ContextClosedEvent> {

    private final AssignmentRecurrenceGenerationService generationService;
    private final AssignmentRecurrenceProperties properties;
    private final AssignmentRecurrenceLogger logger;
    private volatile boolean stopping;

    @Scheduled(fixedDelayString = "${assignment.recurrence.fixed-delay:1m}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process() {
        if (!properties.enabled()) {
            return;
        }
        for (AssignmentRecurrenceCandidate candidate : generationService.findDueCandidates(properties.batchSize())) {
            if (stopping) {
                return;
            }
            generate(candidate);
        }
    }

    // 종료가 시작되면 지금 규칙만 마치고 남은 규칙은 다음 실행에 맡긴다. 생성 시각이 지난 규칙은 다시 대상이 된다.
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        stopping = true;
    }

    // 한 규칙의 실패가 같은 배치의 다른 규칙을 막지 않도록 규칙마다 따로 처리한다.
    private void generate(AssignmentRecurrenceCandidate candidate) {
        try {
            generationService.generate(candidate);
        } catch (DataIntegrityViolationException exception) {
            logger.occurrenceExists(candidate.recurrenceId(), exception);
            recover(() -> generationService.skipExistingOccurrence(candidate), candidate);
        } catch (RuntimeException exception) {
            logger.generationFailed(candidate.recurrenceId(), exception);
            recover(() -> generationService.postpone(candidate, properties.retryDelay()), candidate);
        }
    }

    // 실패한 트랜잭션은 이미 롤백됐으므로 복구는 새 트랜잭션에서 한다. 복구마저 실패하면 다음 주기에 다시 시도한다.
    private void recover(Runnable recovery, AssignmentRecurrenceCandidate candidate) {
        try {
            recovery.run();
        } catch (RuntimeException exception) {
            logger.generationFailed(candidate.recurrenceId(), exception);
        }
    }
}
