package withoutc.chongchong.assignment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.study.entity.StudyMember;

class AssignmentTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Instant INSTANT = Instant.parse("2026-08-20T01:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZONE_ID);

    private final StudyMember writer = mock(StudyMember.class);

    @Test
    @DisplayName("과제 생성 시 제목, 내용, 제출 방법을 검증한다")
    void createWithInvalidTextTest() {
        assertInvalidCreate(" ", "과제 내용", "링크 제출", AssignmentErrorCode.INVALID_TITLE);
        assertInvalidCreate("과제 제목", " ", "링크 제출", AssignmentErrorCode.INVALID_CONTENT);
        assertInvalidCreate("과제 제목", "과제 내용", " ", AssignmentErrorCode.INVALID_SUBMISSION_METHOD);
    }

    @Test
    @DisplayName("제출 방법, 마감 시각, 리마인드 시각은 각각의 오류 코드로 구분한다")
    void distinguishFieldValidationErrorCodesTest() {
        assertErrorCode(
                () -> createAssignment("과제 제목", "과제 내용", " ", NOW.plusHours(1)),
                "INVALID_SUBMISSION_METHOD"
        );
        assertErrorCode(
                () -> createAssignment("과제 제목", "과제 내용", "링크 제출", null),
                "INVALID_CLOSE_AT"
        );

        Assignment assignment = createAssignment();
        assertErrorCode(() -> assignment.addReminders(List.of(NOW), NOW), "INVALID_REMIND_AT");
    }

    @Test
    @DisplayName("과제 제목은 15자까지 허용하고 15자를 초과하면 거부한다")
    void validateTitleLengthTest() {
        assertThatCode(() -> createAssignment("가".repeat(15), "과제 내용", "링크 제출", NOW.plusHours(1)))
                .doesNotThrowAnyException();

        assertInvalidCreate("가".repeat(16), "과제 내용", "링크 제출", AssignmentErrorCode.INVALID_TITLE);
    }

    @Test
    @DisplayName("과제 내용은 10000자까지 허용하고 10000자를 초과하면 거부한다")
    void validateContentLengthTest() {
        assertThatCode(() -> createAssignment("과제 제목", "가".repeat(10000), "링크 제출", NOW.plusHours(1)))
                .doesNotThrowAnyException();

        assertInvalidCreate("과제 제목", "가".repeat(10001), "링크 제출", AssignmentErrorCode.INVALID_CONTENT);
    }

    @Test
    @DisplayName("제출 방법은 10000자까지 허용하고 10000자를 초과하면 거부한다")
    void validateSubmissionMethodLengthTest() {
        assertThatCode(() -> createAssignment("과제 제목", "과제 내용", "가".repeat(10000), NOW.plusHours(1)))
                .doesNotThrowAnyException();

        assertInvalidCreate("과제 제목", "과제 내용", "가".repeat(10001),
                AssignmentErrorCode.INVALID_SUBMISSION_METHOD);
    }

    @Test
    @DisplayName("마감 시각은 고정된 현재 시각 이전과 현재를 거부하고 미래를 허용한다")
    void validateCloseAtBoundaryTest() {
        assertInvalidCreate("과제 제목", "과제 내용", "링크 제출", AssignmentErrorCode.INVALID_CLOSE_AT, null);
        assertInvalidCreate("과제 제목", "과제 내용", "링크 제출", AssignmentErrorCode.INVALID_CLOSE_AT,
                NOW.minusNanos(1));
        assertInvalidCreate("과제 제목", "과제 내용", "링크 제출", AssignmentErrorCode.INVALID_CLOSE_AT, NOW);
        assertThatCode(() -> createAssignment("과제 제목", "과제 내용", "링크 제출", NOW.plusNanos(1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("과제 수정 시 전달된 필드만 바꾸고 마감 시각도 변경한다")
    void updateOnlyProvidedFieldsIncludingCloseAtTest() {
        LocalDateTime originalCloseAt = NOW.plusDays(1);
        LocalDateTime updatedCloseAt = NOW.plusDays(2);
        Assignment assignment = createAssignment("기존 제목", "기존 내용", "기존 방법", originalCloseAt);

        assignment.update("수정 제목", null, "수정 방법", updatedCloseAt, null, NOW);

        assertThat(assignment.getTitle()).isEqualTo("수정 제목");
        assertThat(assignment.getContent()).isEqualTo("기존 내용");
        assertThat(assignment.getSubmissionMethod()).isEqualTo("수정 방법");
        assertThat(assignment.getCloseAt()).isEqualTo(updatedCloseAt);
    }

    @Test
    @DisplayName("과제 수정 시 공백 텍스트와 과거 마감 시각을 거부한다")
    void updateWithInvalidValuesTest() {
        Assignment assignment = createAssignment("기존 제목", "기존 내용", "기존 방법", NOW.plusDays(1));

        assertThatThrownBy(() -> assignment.update(" ", null, null, null, null, NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_TITLE);
        assertThatThrownBy(() -> assignment.update(null, " ", null, null, null, NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_CONTENT);
        assertThatThrownBy(() -> assignment.update(null, null, null, NOW.minusNanos(1), null, NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_CLOSE_AT);
    }

    @Test
    @DisplayName("리마인드 시각을 설정하지 않으면 리마인더를 추가하지 않는다")
    void addNullRemindersTest() {
        Assignment assignment = createAssignment();

        assignment.addReminders(null, NOW);

        assertThat(assignment.getReminders()).isEmpty();
    }

    @Test
    @DisplayName("동일한 리마인드 시각은 한 번만 추가한다")
    void addDistinctRemindersTest() {
        Assignment assignment = createAssignment();
        LocalDateTime remindAt = NOW.plusHours(1);

        assignment.addReminders(List.of(remindAt, remindAt), NOW);

        assertThat(assignment.getReminders()).hasSize(1);
    }

    @Test
    @DisplayName("null 또는 미래가 아닌 시각의 리마인더는 추가할 수 없다")
    void addNonFutureReminderTest() {
        Assignment assignment = createAssignment();

        assertInvalidRemindAt(assignment, Collections.singletonList(null));
        assertInvalidRemindAt(assignment, List.of(NOW.minusNanos(1)));
        assertInvalidRemindAt(assignment, List.of(NOW));
    }

    @Test
    @DisplayName("리마인더 교체 시 발송 완료 리마인더는 유지하고 대기 리마인더만 교체한다")
    void replacePendingRemindersPreservesSentReminderTest() {
        Assignment assignment = createAssignment();
        LocalDateTime sentRemindAt = NOW.plusHours(1);
        LocalDateTime pendingRemindAt = NOW.plusHours(2);
        LocalDateTime newRemindAt = NOW.plusHours(3);
        assignment.addReminders(List.of(sentRemindAt, pendingRemindAt), NOW);
        assignment.getReminders().getFirst().markAsSent();

        assignment.update(null, null, null, null, List.of(newRemindAt, newRemindAt), NOW);

        assertThat(assignment.getReminders())
                .extracting(AssignmentReminder::getRemindAt)
                .containsExactlyInAnyOrder(sentRemindAt, newRemindAt);
        assertThat(assignment.getNextRemindAt()).isEqualTo(newRemindAt);
    }

    @Test
    @DisplayName("교체할 리마인드 시각이 잘못되면 기존 대기 리마인더를 유지한다")
    void replacePendingRemindersKeepsExistingRemindersOnInvalidInputTest() {
        Assignment assignment = createAssignment();
        LocalDateTime existingRemindAt = NOW.plusHours(1);
        assignment.addReminders(List.of(existingRemindAt), NOW);

        assertThatThrownBy(() -> assignment.update(null, null, null, null, List.of(NOW.plusHours(2), NOW), NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_REMIND_AT);

        assertThat(assignment.getReminders())
                .extracting(AssignmentReminder::getRemindAt)
                .containsExactly(existingRemindAt);
    }

    @Test
    @DisplayName("같은 member id의 과제 제출 정보는 한 번만 초기화하고 저장 전 스터디원은 제외한다")
    void initializeDistinctSubmissionsBySavedMemberIdTest() {
        StudyMember firstMember = memberWithId(1L);
        StudyMember duplicateMember = memberWithId(1L);
        StudyMember unsavedMember = memberWithId(null);
        Assignment assignment = createAssignment();

        assignment.initializeSubmissions(List.of(firstMember, duplicateMember, unsavedMember));
        assignment.initializeSubmissions(List.of(duplicateMember));

        assertThat(assignment.getSubmissions())
                .singleElement()
                .extracting(AssignmentSubmission::getMember)
                .isSameAs(firstMember);
    }

    @Test
    @DisplayName("제출 수와 제출 완료 수를 계산하고 가장 이른 대기 리마인더를 반환한다")
    void calculateSubmissionCountsAndNextReminderTest() {
        Assignment assignment = createAssignment();
        assignment.initializeSubmissions(List.of(memberWithId(1L), memberWithId(2L)));
        ReflectionTestUtils.setField(assignment.getSubmissions().getFirst(), "submitted", true);

        LocalDateTime firstRemindAt = NOW.plusHours(1);
        LocalDateTime nextRemindAt = NOW.plusHours(2);
        assignment.addReminders(List.of(firstRemindAt, nextRemindAt), NOW);
        assignment.getReminders().getFirst().markAsSent();

        assertThat(assignment.getSubmissionCount()).isEqualTo(2);
        assertThat(assignment.getSubmittedCount()).isEqualTo(1);
        assertThat(assignment.getNextRemindAt()).isEqualTo(nextRemindAt);
    }

    private Assignment createAssignment() {
        return createAssignment("과제 제목", "과제 내용", "링크 제출", NOW.plusDays(1));
    }

    private Assignment createAssignment(String title, String content, String submissionMethod, LocalDateTime closeAt) {
        return Assignment.create(writer, title, content, submissionMethod, closeAt, NOW);
    }

    private StudyMember memberWithId(Long id) {
        StudyMember member = mock(StudyMember.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    private void assertInvalidCreate(String title, String content, String submissionMethod, AssignmentErrorCode errorCode) {
        assertInvalidCreate(title, content, submissionMethod, errorCode, NOW.plusHours(1));
    }

    private void assertInvalidCreate(String title, String content, String submissionMethod, AssignmentErrorCode errorCode,
                                     LocalDateTime closeAt) {
        assertThatThrownBy(() -> createAssignment(title, content, submissionMethod, closeAt))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }

    private void assertInvalidRemindAt(Assignment assignment, List<LocalDateTime> remindAts) {
        assertThatThrownBy(() -> assignment.addReminders(remindAts, NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_REMIND_AT);
    }

    private void assertErrorCode(ThrowingCallable callable, String expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(AssignmentException.class)
                .satisfies(exception -> {
                    AssignmentErrorCode errorCode = (AssignmentErrorCode) ((AssignmentException) exception)
                            .getErrorCode();
                    assertThat(errorCode.name()).isEqualTo(expectedCode);
                    assertThat(errorCode.getCode()).isEqualTo(expectedCode);
                });
    }
}
