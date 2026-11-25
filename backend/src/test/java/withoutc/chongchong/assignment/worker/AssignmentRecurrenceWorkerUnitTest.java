package withoutc.chongchong.assignment.worker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import withoutc.chongchong.assignment.config.AssignmentRecurrenceProperties;
import withoutc.chongchong.assignment.logging.AssignmentRecurrenceLogger;
import withoutc.chongchong.assignment.repository.projection.AssignmentRecurrenceCandidate;
import withoutc.chongchong.assignment.service.AssignmentRecurrenceGenerationService;

class AssignmentRecurrenceWorkerUnitTest {

    private static final AssignmentRecurrenceCandidate FIRST = new AssignmentRecurrenceCandidate(1L, 10L);
    private static final AssignmentRecurrenceCandidate SECOND = new AssignmentRecurrenceCandidate(2L, 10L);

    private final AssignmentRecurrenceGenerationService generationService =
            mock(AssignmentRecurrenceGenerationService.class);
    private final AssignmentRecurrenceLogger logger = mock(AssignmentRecurrenceLogger.class);

    @Test
    @DisplayName("워커를 끄면 후보를 조회하지 않는다")
    void doNothingWhenDisabled() {
        worker(false).process();

        verify(generationService, never()).findDueCandidates(anyInt());
    }

    @Test
    @DisplayName("한 규칙이 실패하면 그 규칙만 미루고 다음 규칙은 계속 처리한다")
    void postponeFailedRecurrenceAndContinue() {
        when(generationService.findDueCandidates(100)).thenReturn(List.of(FIRST, SECOND));
        doThrow(new IllegalStateException("고장")).when(generationService).generate(FIRST);

        worker(true).process();

        verify(logger).generationFailed(any(), any());
        verify(generationService).postpone(FIRST, Duration.ofMinutes(10));
        verify(generationService).generate(SECOND);
    }

    @Test
    @DisplayName("같은 회차가 이미 있으면 미루지 않고 그 회차를 건너뛴다")
    void skipExistingOccurrence() {
        when(generationService.findDueCandidates(100)).thenReturn(List.of(FIRST));
        doThrow(new DataIntegrityViolationException("중복")).when(generationService).generate(FIRST);

        worker(true).process();

        verify(generationService).skipExistingOccurrence(FIRST);
        verify(generationService, never()).postpone(any(), any());
    }

    @Test
    @DisplayName("종료가 시작되면 지금 규칙만 마치고 남은 규칙은 다음 실행에 맡긴다")
    void stopAfterCurrentRecurrenceWhenContextCloses() {
        AssignmentRecurrenceWorker worker = worker(true);
        when(generationService.findDueCandidates(100)).thenReturn(List.of(FIRST, SECOND));
        doAnswer(invocation -> {
            worker.onApplicationEvent(null);
            return null;
        }).when(generationService).generate(FIRST);

        worker.process();

        verify(generationService).generate(FIRST);
        verify(generationService, never()).generate(SECOND);
    }

    @Test
    @DisplayName("설정값이 잘못되면 앱이 시작되지 않도록 거부한다")
    void rejectInvalidProperties() {
        assertThatThrownBy(() -> new AssignmentRecurrenceProperties(true, Duration.ZERO, 100, Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AssignmentRecurrenceProperties(true, Duration.ofMinutes(1), 0,
                Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AssignmentRecurrenceWorker worker(boolean enabled) {
        AssignmentRecurrenceProperties properties = new AssignmentRecurrenceProperties(enabled,
                Duration.ofMinutes(1), 100, Duration.ofMinutes(10));
        return new AssignmentRecurrenceWorker(generationService, properties, logger);
    }
}
