package withoutc.chongchong.assignment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

class AssignmentRecurrenceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);
    private static final LocalDateTime FIRST_CLOSE_AT = LocalDateTime.of(2026, 12, 1, 23, 59);
    private static final List<LocalDateTime> FIRST_REMIND_ATS = List.of(
            LocalDateTime.of(2026, 11, 30, 23, 59), LocalDateTime.of(2026, 12, 1, 22, 59));

    private final Study study = mock(Study.class);
    private final List<StudyMember> submitters = List.of(memberWithId(1L), memberWithId(2L));

    @Test
    @DisplayName("첫 회차를 템플릿으로 삼고 리마인드 시각을 마감 기준 분 단위로 바꿔 저장한다")
    void startWithFirstOccurrence() {
        AssignmentRecurrence recurrence = AssignmentRecurrence.startWith(firstOccurrence(FIRST_REMIND_ATS), 1,
                SubmitterMode.RANGE);

        assertThat(recurrence.getTitle()).isEqualTo("알고리즘 풀이");
        assertThat(recurrence.getSubmissionTarget()).isEqualTo(SubmissionTarget.MEMBERS_ONLY);
        assertThat(recurrence.getRemindOffsets()).containsExactlyInAnyOrder(24 * 60, 60);
        assertThat(recurrence.getSubmitterIds()).isEmpty();
    }

    @Test
    @DisplayName("직접 선택한 규칙은 제출 대상 범위 대신 멤버 ID를 저장한다")
    void startWithSelectedSubmitters() {
        AssignmentRecurrence recurrence = AssignmentRecurrence.startWith(firstOccurrence(List.of()), 1,
                SubmitterMode.SELECTED);

        assertThat(recurrence.selectsSubmitters()).isTrue();
        assertThat(recurrence.getSubmissionTarget()).isNull();
        assertThat(recurrence.getSubmitterIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("반복 간격 이상 앞선 리마인드는 다음 회차가 만들어지기 전 시각이라 거부한다")
    void rejectRemindOffsetLongerThanInterval() {
        Assignment firstOccurrence = firstOccurrence(List.of(FIRST_CLOSE_AT.minusWeeks(1)));

        assertThatThrownBy(() -> AssignmentRecurrence.startWith(firstOccurrence, 1, SubmitterMode.RANGE))
                .isInstanceOf(AssignmentException.class)
                .extracting("errorCode")
                .isEqualTo(AssignmentErrorCode.INVALID_REMIND_AT);
    }

    @Test
    @DisplayName("다음 회차는 규칙의 일정으로 마감과 리마인드를 계산하고 규칙에 연결된다")
    void createOccurrence() {
        AssignmentRecurrence recurrence = startedRecurrence();

        Assignment occurrence = recurrence.createOccurrence(submitters, FIRST_CLOSE_AT);

        assertThat(occurrence.getCloseAt()).isEqualTo(LocalDateTime.of(2026, 12, 8, 23, 59));
        assertThat(occurrence.getRecurrenceId()).isEqualTo(10L);
        assertThat(occurrence.getOccurrenceDate()).isEqualTo(LocalDate.of(2026, 12, 8));
        assertThat(occurrence.getSubmitterIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(occurrence.getPendingRemindAts(FIRST_CLOSE_AT)).containsExactly(
                LocalDateTime.of(2026, 12, 7, 23, 59), LocalDateTime.of(2026, 12, 8, 22, 59));
    }

    @Test
    @DisplayName("회차를 늦게 만들어 이미 지난 리마인드는 만들지 않는다")
    void dropPastRemindAts() {
        AssignmentRecurrence recurrence = startedRecurrence();
        LocalDateTime lateNow = LocalDateTime.of(2026, 12, 8, 12, 0);

        Assignment occurrence = recurrence.createOccurrence(submitters, lateNow);

        assertThat(occurrence.getPendingRemindAts(lateNow)).containsExactly(LocalDateTime.of(2026, 12, 8, 22, 59));
    }

    @Test
    @DisplayName("다음 회차 기준 리마인드 시각을 마감 기준 상대 시간으로 교체한다")
    void replaceRemindAts() {
        AssignmentRecurrence recurrence = startedRecurrence();

        recurrence.changeSchedule(null, null, List.of(LocalDateTime.of(2026, 12, 8, 20, 59)));

        assertThat(recurrence.getRemindOffsets()).containsExactly(180);
        assertThat(recurrence.nextRemindAts()).containsExactly(LocalDateTime.of(2026, 12, 8, 20, 59));
    }

    @Test
    @DisplayName("간격만 보내고 다음 회차 마감을 보내지 않으면 일정을 바꾸지 않고 거부한다")
    void rejectIntervalWithoutNextCloseAt() {
        AssignmentRecurrence recurrence = startedRecurrence();

        assertThatThrownBy(() -> recurrence.changeSchedule(null, 2, null))
                .isInstanceOf(AssignmentException.class)
                .extracting("errorCode")
                .isEqualTo(AssignmentErrorCode.INVALID_CLOSE_AT);
    }

    @Test
    @DisplayName("간격과 리마인드를 함께 줄이면 바뀐 최종 상태를 기준으로 검사한다")
    void shrinkIntervalWithReminders() {
        AssignmentRecurrence recurrence = AssignmentRecurrence.startWith(
                firstOccurrence(List.of(FIRST_CLOSE_AT.minusDays(10))), 4, SubmitterMode.RANGE);
        LocalDateTime nextCloseAt = LocalDateTime.of(2026, 12, 8, 23, 59);

        recurrence.changeSchedule(nextCloseAt, 1, List.of(nextCloseAt.minusDays(1)));

        assertThat(recurrence.getSchedule().getIntervalWeeks()).isEqualTo(1);
        assertThat(recurrence.getRemindOffsets()).containsExactly(24 * 60);
    }

    @Test
    @DisplayName("간격만 줄여 기존 리마인드가 새 간격을 넘으면 거부한다")
    void rejectShrinkIntervalKeepingLongReminder() {
        AssignmentRecurrence recurrence = AssignmentRecurrence.startWith(
                firstOccurrence(List.of(FIRST_CLOSE_AT.minusDays(10))), 4, SubmitterMode.RANGE);

        assertThatThrownBy(() -> recurrence.changeSchedule(LocalDateTime.of(2026, 12, 8, 23, 59), 1, null))
                .isInstanceOf(AssignmentException.class)
                .extracting("errorCode")
                .isEqualTo(AssignmentErrorCode.INVALID_REMIND_AT);
    }

    @Test
    @DisplayName("직접 선택과 범위 지정을 오가면 반대 방식의 값을 비운다")
    void switchSubmitterMode() {
        AssignmentRecurrence recurrence = startedRecurrence();

        recurrence.selectSubmitters(List.of(2L));
        assertThat(recurrence.getSubmissionTarget()).isNull();
        assertThat(recurrence.getSubmitterIds()).containsExactly(2L);

        recurrence.useRange(SubmissionTarget.MEMBERS_AND_LEADER);
        assertThat(recurrence.selectsSubmitters()).isFalse();
        assertThat(recurrence.getSubmitterIds()).isEmpty();
    }

    private AssignmentRecurrence startedRecurrence() {
        AssignmentRecurrence recurrence = AssignmentRecurrence.startWith(firstOccurrence(FIRST_REMIND_ATS), 1,
                SubmitterMode.RANGE);
        ReflectionTestUtils.setField(recurrence, "id", 10L);
        return recurrence;
    }

    private Assignment firstOccurrence(List<LocalDateTime> remindAts) {
        Assignment assignment = Assignment.create(study, "알고리즘 풀이", "이번 주 문제를 풀어요.", "링크 제출",
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY, FIRST_CLOSE_AT, NOW, submitters);
        assignment.addReminders(remindAts, NOW);
        return assignment;
    }

    private static StudyMember memberWithId(Long id) {
        StudyMember member = mock(StudyMember.class);
        when(member.getId()).thenReturn(id);
        return member;
    }
}
