package withoutc.chongchong.assignment.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.user.entity.User;

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

    private static Stream<Arguments> leaderOnlyActions() {
        return Stream.of(
                arguments("과제 생성", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanCreateAssignment),
                arguments("과제 수정", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanUpdateAssignment),
                arguments("과제 삭제", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanDeleteAssignment),
                arguments("제출 현황 조회", (BiConsumer<AssignmentAccessPolicy, StudyMember>)
                        AssignmentAccessPolicy::requireCanReadAssignmentSubmissionStatus)
        );
    }

    @ParameterizedTest(name = "{0}, 제출={1}, 조회자={2}: 허용={3}")
    @CsvSource({
            "LEADER_ONLY, false, LEADER, true",
            "LEADER_ONLY, true, LEADER, true",
            "LEADER_ONLY, false, OWNER, true",
            "LEADER_ONLY, true, OWNER, true",
            "LEADER_ONLY, false, MEMBER, false",
            "LEADER_ONLY, true, MEMBER, false",
            "ALL_STUDY_MEMBERS, false, LEADER, true",
            "ALL_STUDY_MEMBERS, true, LEADER, true",
            "ALL_STUDY_MEMBERS, false, OWNER, true",
            "ALL_STUDY_MEMBERS, true, OWNER, true",
            "ALL_STUDY_MEMBERS, false, MEMBER, false",
            "ALL_STUDY_MEMBERS, true, MEMBER, true"
    })
    @DisplayName("공개 범위와 제출 상태 및 조회자에 따라 상세 조회 권한을 구분한다")
    void readSubmissionPermissions(SubmissionVisibility visibility, boolean submitted, String actorType,
                                   boolean allowed) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 10, 0);
        Study study = Study.create("스터디", "설명");
        Assignment assignment = Assignment.create(study, "과제", "내용", "링크", SubmissionTarget.MEMBERS_ONLY,
                visibility, now.plusDays(1), now);
        StudyMember owner = member(study, 1L, StudyMemberRole.MEMBER);
        StudyMember actor = switch (actorType) {
            case "OWNER" -> owner;
            case "LEADER" -> member(study, 2L, StudyMemberRole.LEADER);
            case "MEMBER" -> member(study, 3L, StudyMemberRole.MEMBER);
            default -> throw new IllegalArgumentException(actorType);
        };
        AssignmentSubmission submission = AssignmentSubmission.create(owner, assignment);
        if (submitted) {
            submission.submit("제출 내용", null, now);
        } else {
            submission.update("아직 제출하지 않은 내용", null);
        }

        Runnable action = () -> policy.requireCanReadSubmission(actor, assignment, submission);
        if (allowed) {
            assertThatCode(action::run).doesNotThrowAnyException();
        } else {
            assertAccessDenied(action);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "LEADER_ONLY, LEADER, true",
            "LEADER_ONLY, MEMBER, false",
            "ALL_STUDY_MEMBERS, LEADER, true",
            "ALL_STUDY_MEMBERS, MEMBER, true"
    })
    @DisplayName("공개 과제 목록은 일반 멤버도 조회하고 비공개 목록은 리더만 조회한다")
    void readSubmissionListPermissions(SubmissionVisibility visibility, StudyMemberRole role, boolean allowed) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 10, 0);
        Study study = Study.create("스터디", "설명");
        Assignment assignment = Assignment.create(study, "과제", "내용", "링크", SubmissionTarget.MEMBERS_ONLY,
                visibility, now.plusDays(1), now);
        StudyMember actor = member(study, 1L, role);

        Runnable action = () -> policy.requireCanReadSubmissionList(assignment, actor);
        if (allowed) {
            assertThatCode(action::run).doesNotThrowAnyException();
        } else {
            assertAccessDenied(action);
        }
    }

    private StudyMember member(Study study, Long id, StudyMemberRole role) {
        StudyMember member = StudyMember.create(study, User.create("사용자", null), "사용자", null, role);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private void assertAccessDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);
    }
}
