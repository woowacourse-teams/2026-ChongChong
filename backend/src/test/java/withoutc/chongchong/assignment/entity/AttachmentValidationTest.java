package withoutc.chongchong.assignment.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.file.entity.FileMetadata;

class AttachmentValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"target", "file", "order"})
    @DisplayName("잘못된 과제 첨부 값은 과제 도메인 예외로 거부한다")
    void rejectsInvalidAssignmentAttachment(String invalidField) {
        Assignment assignment = invalidField.equals("target") ? null : mock(Assignment.class);
        FileMetadata file = invalidField.equals("file") ? null : mock(FileMetadata.class);
        int sortOrder = invalidField.equals("order") ? -1 : 0;

        assertThatThrownBy(() -> AssignmentAttachment.create(assignment, file, sortOrder))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_ATTACHMENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"target", "file", "order"})
    @DisplayName("잘못된 제출물 첨부 값은 과제 도메인 예외로 거부한다")
    void rejectsInvalidSubmissionAttachment(String invalidField) {
        AssignmentSubmission submission = invalidField.equals("target") ? null : mock(AssignmentSubmission.class);
        FileMetadata file = invalidField.equals("file") ? null : mock(FileMetadata.class);
        int sortOrder = invalidField.equals("order") ? -1 : 0;

        assertThatThrownBy(() -> SubmissionAttachment.create(submission, file, sortOrder))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_ATTACHMENT);
    }
}
