package withoutc.chongchong.assignment.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.StudyMember;

class AssignmentAccessPolicyTest {

    private final AssignmentAccessPolicy policy = new AssignmentAccessPolicy();

    @ParameterizedTest(name = "{0}: 리더는 허용한다")
    @MethodSource("leaderOnlyActions")
    void allowLeaderTest(String name, BiConsumer<AssignmentAccessPolicy, StudyMember> action) {
        StudyMember leader = mock(StudyMember.class);
        when(leader.isLeader()).thenReturn(true);

        assertThatCode(() -> action.accept(policy, leader)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}: 일반 멤버는 거부한다")
    @MethodSource("leaderOnlyActions")
    void rejectMemberTest(String name, BiConsumer<AssignmentAccessPolicy, StudyMember> action) {
        StudyMember member = mock(StudyMember.class);

        assertAccessDenied(() -> action.accept(policy, member));
    }

    @Test
    @DisplayName("제출물 소유자는 제출물을 수정할 수 있다")
    void allowSubmissionOwnerToUpdateTest() {
        StudyMember owner = mock(StudyMember.class);
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        when(submission.isOwnedBy(owner)).thenReturn(true);

        assertThatCode(() -> policy.requireCanUpdateSubmission(owner, submission))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("제출물 소유자가 아니면 제출물을 수정할 수 없다")
    void rejectNonOwnerFromUpdatingSubmissionTest() {
        StudyMember actor = mock(StudyMember.class);
        AssignmentSubmission submission = mock(AssignmentSubmission.class);

        assertAccessDenied(() -> policy.requireCanUpdateSubmission(actor, submission));
    }

    @Test
    @DisplayName("리더는 다른 멤버의 제출물을 조회할 수 있다")
    void allowLeaderToReadSubmissionTest() {
        StudyMember leader = mock(StudyMember.class);
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        when(leader.isLeader()).thenReturn(true);

        assertThatCode(() -> policy.requireCanReadSubmission(leader, submission))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("일반 멤버는 자신의 제출물을 조회할 수 있다")
    void allowOwnerToReadSubmissionTest() {
        StudyMember owner = mock(StudyMember.class);
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        when(submission.isOwnedBy(owner)).thenReturn(true);

        assertThatCode(() -> policy.requireCanReadSubmission(owner, submission))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("일반 멤버는 다른 멤버의 제출물을 조회할 수 없다")
    void rejectNonOwnerFromReadingSubmissionTest() {
        StudyMember actor = mock(StudyMember.class);
        AssignmentSubmission submission = mock(AssignmentSubmission.class);

        assertAccessDenied(() -> policy.requireCanReadSubmission(actor, submission));
    }

    private static Stream<Arguments> leaderOnlyActions() {
        return Stream.of(
                arguments("과제 생성", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanCreateAssignment),
                arguments("과제 수정", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanUpdateAssignment),
                arguments("과제 삭제", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanDeleteAssignment),
                arguments("제출 현황 조회", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanReadAssignmentSubmissionStatus),
                arguments("제출 목록 조회", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanReadSubmissionList)
        );
    }

    private void assertAccessDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);
    }
}
