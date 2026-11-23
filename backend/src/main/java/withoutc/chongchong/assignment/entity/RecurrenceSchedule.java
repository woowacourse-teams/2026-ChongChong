package withoutc.chongchong.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurrenceSchedule {

    private static final int MIN_INTERVAL_WEEKS = 1;
    private static final int MAX_INTERVAL_WEEKS = 4;
    private static final long MINUTES_PER_WEEK = 7L * 24 * 60;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "interval_weeks", nullable = false)
    private int intervalWeeks;

    @Column(name = "next_occurrence_date", nullable = false)
    private LocalDate nextOccurrenceDate;

    @Column(name = "next_publish_at", nullable = false)
    private LocalDateTime nextPublishAt;

    // 첫 회차를 만든 직후의 일정이다. 다음 회차는 첫 회차가 마감될 때 만든다.
    public static RecurrenceSchedule startAfter(LocalDateTime firstCloseAt, int intervalWeeks) {
        validateIntervalWeeks(intervalWeeks);
        return new RecurrenceSchedule(firstCloseAt.toLocalTime(), intervalWeeks,
                firstCloseAt.toLocalDate().plusWeeks(intervalWeeks), firstCloseAt);
    }

    public LocalDateTime nextCloseAt() {
        return nextOccurrenceDate.atTime(closeTime);
    }

    public int skipPastOccurrences(LocalDateTime now) {
        int skipped = 0;
        while (!nextCloseAt().isAfter(now)) {
            nextOccurrenceDate = nextOccurrenceDate.plusWeeks(intervalWeeks);
            skipped++;
        }
        return skipped;
    }

    // 다음 생성 시각은 방금 처리한 회차의 규칙상 마감이다.
    public void advance() {
        nextPublishAt = nextCloseAt();
        nextOccurrenceDate = nextOccurrenceDate.plusWeeks(intervalWeeks);
    }

    // 다음 회차부터 새 일정을 적용한다. 지금 열린 회차가 마감될 때 다음 회차를 만드는 시각은 그대로 둔다.
    public void reschedule(LocalDateTime nextCloseAt, int intervalWeeks) {
        validateIntervalWeeks(intervalWeeks);
        if (!nextCloseAt.isAfter(nextPublishAt)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_CLOSE_AT);
        }
        this.closeTime = nextCloseAt.toLocalTime();
        this.intervalWeeks = intervalWeeks;
        this.nextOccurrenceDate = nextCloseAt.toLocalDate();
    }

    public void postponeGeneration(LocalDateTime until) {
        nextPublishAt = until;
    }

    public boolean allowsRemindOffset(long minutesBeforeClose) {
        return minutesBeforeClose > 0 && minutesBeforeClose < intervalWeeks * MINUTES_PER_WEEK;
    }

    private static void validateIntervalWeeks(int intervalWeeks) {
        if (intervalWeeks < MIN_INTERVAL_WEEKS || intervalWeeks > MAX_INTERVAL_WEEKS) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_RECURRENCE_INTERVAL);
        }
    }

    private RecurrenceSchedule(LocalTime closeTime, int intervalWeeks, LocalDate nextOccurrenceDate,
                               LocalDateTime nextPublishAt) {
        this.closeTime = closeTime;
        this.intervalWeeks = intervalWeeks;
        this.nextOccurrenceDate = nextOccurrenceDate;
        this.nextPublishAt = nextPublishAt;
    }
}
