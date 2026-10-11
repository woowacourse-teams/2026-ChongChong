package withoutc.chongchong.assignment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.study.entity.StudyMember;

class AssignmentSubmissionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 10, 0);

    @Test
    @DisplayName("내용과 링크가 없어도 제출 완료 상태로 변경한다")
    void submitWithoutContentAndLinkTest() {
        AssignmentSubmission submission = createSubmission();

        submission.submit(null, null, NOW);

        assertThat(submission.submissionStatus(NOW)).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submission.getContent()).isNull();
        assertThat(submission.getLink()).isNull();
        assertThat(submission.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("제출 내용이 최대 길이를 초과하면 기존 상태를 유지한다")
    void rejectContentOverMaximumLengthTest() {
        AssignmentSubmission submission = createSubmission();

        assertThatThrownBy(() -> submission.submit("a".repeat(10001), null, NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_CONTENT);
        assertThat(submission.submissionStatus(NOW)).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(submission.getContent()).isNull();
    }

    @Test
    @DisplayName("제출 링크가 최대 길이를 초과하면 기존 상태를 유지한다")
    void rejectLinkOverMaximumLengthTest() {
        AssignmentSubmission submission = createSubmission();

        assertThatThrownBy(() -> submission.submit(null, "a".repeat(10001), NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_LINK);
        assertThat(submission.submissionStatus(NOW)).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(submission.getLink()).isNull();
    }

    @Test
    @DisplayName("수정 값이 없으면 기존 제출 내용을 유지한다")
    void updateOnlyProvidedValueTest() {
        AssignmentSubmission submission = createSubmission();
        submission.submit("기존 내용", "https://old.example.com", NOW);

        submission.update(null, "https://new.example.com");

        assertThat(submission.getContent()).isEqualTo("기존 내용");
        assertThat(submission.getLink()).isEqualTo("https://new.example.com");
        assertThat(submission.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("수정 입력 중 하나가 유효하지 않으면 기존 제출 내용을 모두 유지한다")
    void rejectUpdateWithInvalidLinkKeepsExistingValuesTest() {
        AssignmentSubmission submission = createSubmission();
        submission.submit("기존 내용", "https://old.example.com", NOW);

        assertThatThrownBy(() -> submission.update("새 내용", "a".repeat(10001)))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_LINK);
        assertThat(submission.getContent()).isEqualTo("기존 내용");
        assertThat(submission.getLink()).isEqualTo("https://old.example.com");
    }

    @Test
    @DisplayName("제출 시각이 없으면 미제출 상태로 판단한다")
    void isNotSubmittedWhenSubmittedAtIsNullTest() {
        AssignmentSubmission submission = createSubmission();

        assertThat(submission.submissionStatus(NOW)).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(submission.getSubmittedAt()).isNull();
    }

    @Test
    @DisplayName("소유자와 비교 대상의 식별자가 모두 없으면 소유자로 판단하지 않는다")
    void isNotOwnedByMemberWhenBothIdsAreNullTest() {
        StudyMember owner = mock(StudyMember.class);
        StudyMember actor = mock(StudyMember.class);
        when(owner.getId()).thenReturn(null);
        when(actor.getId()).thenReturn(null);
        AssignmentSubmission submission = AssignmentSubmission.create(owner, mock(Assignment.class));

        assertThat(submission.isOwnedBy(actor)).isFalse();
    }

    @Test
    @DisplayName("비교 대상이 없으면 소유자로 판단하지 않는다")
    void isNotOwnedByNullMemberTest() {
        AssignmentSubmission submission = createSubmission();

        assertThat(submission.isOwnedBy(null)).isFalse();
    }

    @Test
    @DisplayName("소유자와 비교 대상의 식별자가 같으면 소유자로 판단한다")
    void isOwnedByMemberWithSameIdTest() {
        StudyMember owner = mock(StudyMember.class);
        StudyMember actor = mock(StudyMember.class);
        when(owner.getId()).thenReturn(1L);
        when(actor.getId()).thenReturn(1L);
        AssignmentSubmission submission = AssignmentSubmission.create(owner, mock(Assignment.class));

        assertThat(submission.isOwnedBy(actor)).isTrue();
    }

    @Test
    @DisplayName("미제출 과제는 마감 시각을 지난 뒤 미제출 확정 상태가 된다")
    void missingAfterDeadlineTest() {
        AssignmentSubmission submission = createSubmission();
        LocalDateTime closeAt = NOW.plusHours(1);

        assertThat(submission.submissionStatus(closeAt)).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(submission.submissionStatus(closeAt.plusNanos(1))).isEqualTo(SubmissionStatus.MISSING);
        assertThat(submission.isSubmit(closeAt.plusNanos(1))).isFalse();
    }

    @Test
    @DisplayName("마감 시각에 제출하면 이후 조회에도 정상 제출로 유지된다")
    void submittedAtDeadlineTest() {
        AssignmentSubmission submission = createSubmission();
        LocalDateTime closeAt = NOW.plusHours(1);
        submission.submit(null, null, closeAt);

        assertThat(submission.submissionStatus(closeAt.plusDays(1))).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submission.isSubmit(closeAt.plusDays(1))).isTrue();
    }

    @Test
    @DisplayName("마감 후 제출은 지각 제출이며 재제출해도 최초 제출 시각을 유지한다")
    void lateSubmissionKeepsFirstSubmittedAtTest() {
        AssignmentSubmission submission = createSubmission();
        LocalDateTime submittedAt = NOW.plusHours(2);
        submission.submit("최초 내용", null, submittedAt);
        submission.submit("수정 내용", null, submittedAt.plusHours(1));

        assertThat(submission.submissionStatus(submittedAt.plusHours(1))).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
        assertThat(submission.isSubmit(submittedAt.plusHours(1))).isTrue();
        assertThat(submission.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(submission.getContent()).isEqualTo("수정 내용");
    }

    private AssignmentSubmission createSubmission() {
        Assignment assignment = mock(Assignment.class);
        when(assignment.getCloseAt()).thenReturn(NOW.plusHours(1));
        return AssignmentSubmission.create(mock(StudyMember.class), assignment);
    }
}
