package withoutc.chongchong.assignment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;

class RecurrenceScheduleTest {

    private static final LocalDateTime FIRST_CLOSE_AT = LocalDateTime.of(2026, 12, 1, 23, 59);
    private static final long MINUTES_PER_WEEK = 7L * 24 * 60;

    @Test
    @DisplayName("첫 회차 마감 시각에 다음 회차를 만들도록 일정을 시작한다")
    void startAfterFirstOccurrence() {
        RecurrenceSchedule schedule = RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 1);

        assertThat(schedule.getCloseTime()).isEqualTo(LocalTime.of(23, 59));
        assertThat(schedule.getNextOccurrenceDate()).isEqualTo(LocalDate.of(2026, 12, 8));
        assertThat(schedule.getNextPublishAt()).isEqualTo(FIRST_CLOSE_AT);
    }

    @Test
    @DisplayName("회차를 처리하면 그 회차의 마감 시각에 다음 회차를 만들도록 넘어간다")
    void advanceToFollowingOccurrence() {
        RecurrenceSchedule schedule = RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 2);

        schedule.advance();

        assertThat(schedule.getNextPublishAt()).isEqualTo(LocalDateTime.of(2026, 12, 15, 23, 59));
        assertThat(schedule.getNextOccurrenceDate()).isEqualTo(LocalDate.of(2026, 12, 29));
    }

    @Test
    @DisplayName("마감이 지난 회차는 건너뛰고 건너뛴 수를 반환한다")
    void skipPastOccurrences() {
        RecurrenceSchedule schedule = RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 1);

        int skipped = schedule.skipPastOccurrences(LocalDateTime.of(2026, 12, 22, 23, 59));

        assertThat(skipped).isEqualTo(3);
        assertThat(schedule.nextCloseAt()).isEqualTo(LocalDateTime.of(2026, 12, 29, 23, 59));
    }

    @Test
    @DisplayName("다음 회차 마감이 미래면 건너뛰지 않는다")
    void keepFutureOccurrence() {
        RecurrenceSchedule schedule = RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 1);

        assertThat(schedule.skipPastOccurrences(FIRST_CLOSE_AT)).isZero();
        assertThat(schedule.getNextOccurrenceDate()).isEqualTo(LocalDate.of(2026, 12, 8));
    }

    @Test
    @DisplayName("일정을 바꾸면 다음 회차부터 새 요일·시각·간격을 쓰고 다음 생성 시각은 유지한다")
    void reschedule() {
        RecurrenceSchedule schedule = RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 1);

        schedule.reschedule(LocalDateTime.of(2026, 12, 10, 21, 0), 2);

        assertThat(schedule.nextCloseAt()).isEqualTo(LocalDateTime.of(2026, 12, 10, 21, 0));
        assertThat(schedule.getIntervalWeeks()).isEqualTo(2);
        assertThat(schedule.getNextPublishAt()).isEqualTo(FIRST_CLOSE_AT);
    }

    @Test
    @DisplayName("다음 회차 마감은 지금 열린 회차의 마감보다 뒤여야 한다")
    void rejectRescheduleBeforeOpenOccurrenceCloses() {
        RecurrenceSchedule schedule = RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 1);

        assertThatThrownBy(() -> schedule.reschedule(FIRST_CLOSE_AT, 1))
                .isInstanceOf(AssignmentException.class)
                .extracting("errorCode")
                .isEqualTo(AssignmentErrorCode.INVALID_CLOSE_AT);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    @DisplayName("반복 간격은 1주에서 4주 사이만 허용한다")
    void rejectInvalidInterval(int intervalWeeks) {
        assertThatCode(() -> RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 1)).doesNotThrowAnyException();
        assertThatCode(() -> RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 4)).doesNotThrowAnyException();
        assertThatThrownBy(() -> RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, intervalWeeks))
                .isInstanceOf(AssignmentException.class)
                .extracting("errorCode")
                .isEqualTo(AssignmentErrorCode.INVALID_RECURRENCE_INTERVAL);
    }

    @Test
    @DisplayName("리마인드는 마감 전이면서 반복 간격보다 짧은 시간만 허용한다")
    void allowRemindOffsetWithinInterval() {
        RecurrenceSchedule schedule = RecurrenceSchedule.startAfter(FIRST_CLOSE_AT, 1);

        assertThat(schedule.allowsRemindOffset(0)).isFalse();
        assertThat(schedule.allowsRemindOffset(1)).isTrue();
        assertThat(schedule.allowsRemindOffset(MINUTES_PER_WEEK - 1)).isTrue();
        assertThat(schedule.allowsRemindOffset(MINUTES_PER_WEEK)).isFalse();
    }
}
