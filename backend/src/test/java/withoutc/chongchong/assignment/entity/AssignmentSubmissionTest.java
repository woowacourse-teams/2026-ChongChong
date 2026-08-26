package withoutc.chongchong.assignment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
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

        assertThat(submission.isSubmitted()).isTrue();
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
        assertThat(submission.isSubmitted()).isFalse();
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
        assertThat(submission.isSubmitted()).isFalse();
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
    @DisplayName("기존 제출 데이터에 제출 시각이 없으면 마지막 수정 시각을 반환한다")
    void getLegacySubmittedAtTest() {
        AssignmentSubmission submission = createSubmission();
        LocalDateTime legacyUpdatedAt = NOW.minusDays(1);
        ReflectionTestUtils.setField(submission, "submitted", true);
        ReflectionTestUtils.setField(submission, "updatedAt", legacyUpdatedAt);

        assertThat(submission.getSubmittedAt()).isEqualTo(legacyUpdatedAt);
    }

    private AssignmentSubmission createSubmission() {
        return AssignmentSubmission.create(mock(StudyMember.class), mock(Assignment.class));
    }
}
