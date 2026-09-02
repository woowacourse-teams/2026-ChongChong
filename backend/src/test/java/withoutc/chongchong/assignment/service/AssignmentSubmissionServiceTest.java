package withoutc.chongchong.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmissionStatusResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
import withoutc.chongchong.assignment.support.AssignmentTestFixture;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long STUDY_ID = 10L;
    private static final Long ASSIGNMENT_ID = 100L;
    private static final Long MEMBER_ID = 20L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 10, 0);

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private AssignmentAccessPolicy assignmentAccessPolicy;

    private AssignmentSubmissionService assignmentSubmissionService;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"), zoneId);
        assignmentSubmissionService = new AssignmentSubmissionService(
                assignmentRepository,
                assignmentSubmissionRepository,
                studyMemberRepository,
                assignmentAccessPolicy,
                clock
        );
    }

    @Test
    @DisplayName("스터디원이 자신의 과제를 제출하면 해당 스터디원 제출물만 상태를 변경한다")
    void submitAssignmentTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        AssignmentSubmitRequest request = new AssignmentSubmitRequest("제출 내용", "https://example.com");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByAssignmentIdAndMemberIdOrThrow(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(submission);

        AssignmentSubmitResponse response = assignmentSubmissionService.submit(USER_ID, STUDY_ID, ASSIGNMENT_ID,
                request);

        assertThat(response.submissionId()).isEqualTo(300L);
        assertThat(submission.isSubmitted()).isTrue();
        assertThat(submission.getContent()).isEqualTo("제출 내용");
        assertThat(submission.getLink()).isEqualTo("https://example.com");
        assertThat(submission.getSubmittedAt()).isEqualTo(NOW);
        verify(assignmentRepository).getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID);
        verify(assignmentSubmissionRepository).getByAssignmentIdAndMemberIdOrThrow(ASSIGNMENT_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("요청한 스터디에서 과제를 찾지 못하면 제출할 수 없다")
    void submitWhenAssignmentNotFoundInStudyTest() {
        StudyMember member = studyMember(assignmentWithId(200L), MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmitRequest request = new AssignmentSubmitRequest("제출 내용", null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID))
                .thenThrow(new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));

        assertAssignmentNotFound(
                () -> assignmentSubmissionService.submit(USER_ID, STUDY_ID, ASSIGNMENT_ID, request)
        );

        verifyNoInteractions(assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("제출물 수정 권한을 확인한 뒤 변경 내용을 저장한다")
    void updateSubmissionTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        submission.submit("기존 내용", "https://old.example.com", NOW);
        AssignmentSubmitRequest request = new AssignmentSubmitRequest("수정 내용", null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID))
                .thenReturn(submission);

        assignmentSubmissionService.updateSubmission(USER_ID, STUDY_ID, ASSIGNMENT_ID, 300L, request);

        assertThat(submission.getContent()).isEqualTo("수정 내용");
        assertThat(submission.getLink()).isEqualTo("https://old.example.com");
        verify(assignmentAccessPolicy).requireCanUpdateSubmission(member, submission);
        verify(assignmentSubmissionRepository).getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID);
        verify(assignmentSubmissionRepository).save(submission);
    }

    @Test
    @DisplayName("제출물 수정 정책이 거부하면 제출물을 변경하거나 저장하지 않는다")
    void rejectUpdateSubmissionWhenPolicyDeniesTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember actor = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        StudyMember owner = studyMember(assignment, 21L, StudyMemberRole.MEMBER, "제출자");
        AssignmentSubmission submission = submissionWithId(300L, owner, assignment);
        submission.submit("기존 내용", "https://old.example.com", NOW);
        AssignmentSubmitRequest request = new AssignmentSubmitRequest("수정 내용", null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(actor);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID))
                .thenReturn(submission);
        doThrow(new AuthException(AuthErrorCode.ACCESS_DENIED))
                .when(assignmentAccessPolicy).requireCanUpdateSubmission(actor, submission);

        assertAccessDenied(() -> assignmentSubmissionService.updateSubmission(
                USER_ID, STUDY_ID, ASSIGNMENT_ID, 300L, request));

        assertThat(submission.getContent()).isEqualTo("기존 내용");
        assertThat(submission.getLink()).isEqualTo("https://old.example.com");
        verify(assignmentSubmissionRepository, never()).save(submission);
    }

    @Test
    @DisplayName("스터디원은 자신이 제출한 과제 제출 정보를 조회한다")
    void getMySubmissionDetailTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        submission.submit("제출 내용", "https://example.com", NOW);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(Optional.of(submission));

        MySubmissionDetailResponse response = assignmentSubmissionService.getMySubmissionDetail(
                USER_ID, STUDY_ID, ASSIGNMENT_ID
        );

        assertThat(response.submissionId()).isEqualTo(300L);
        assertThat(response.submitted()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.content()).isEqualTo("제출 내용");
        assertThat(response.link()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("스터디원이 과제를 제출하지 않았다면 미제출 상태를 반환한다")
    void getMySubmissionDetailBeforeSubmitTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(Optional.of(submission));

        MySubmissionDetailResponse response = assignmentSubmissionService.getMySubmissionDetail(
                USER_ID, STUDY_ID, ASSIGNMENT_ID
        );

        assertThat(response.submissionId()).isEqualTo(300L);
        assertThat(response.submitted()).isFalse();
        assertThat(response.createdAt()).isNull();
        assertThat(response.content()).isNull();
        assertThat(response.link()).isNull();
    }

    @Test
    @DisplayName("현재 사용자에게 제출 행이 없다면 내 제출 정보는 null이다")
    void getMySubmissionDetailWithoutSubmissionTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember leader = studyMember(assignment, MEMBER_ID, StudyMemberRole.LEADER, "리더");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(Optional.empty());

        MySubmissionDetailResponse response = assignmentSubmissionService.getMySubmissionDetail(
                USER_ID, STUDY_ID, ASSIGNMENT_ID
        );

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("제출물 조회 권한을 확인한 뒤 상세 정보를 반환한다")
    void getSubmissionDetailTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        submission.submit("제출 내용", "https://example.com", NOW);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID))
                .thenReturn(submission);

        SubmissionDetailResponse response = assignmentSubmissionService.getSubmissionDetail(USER_ID, STUDY_ID,
                ASSIGNMENT_ID, 300L);

        assertThat(response.id()).isEqualTo(300L);
        assertThat(response.name()).isEqualTo("스터디원");
        assertThat(response.content()).isEqualTo("제출 내용");
        verify(assignmentAccessPolicy).requireCanReadSubmission(member, submission);
        verify(assignmentSubmissionRepository).getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID);
    }

    @Test
    @DisplayName("제출물 조회 정책이 거부하면 예외를 전파한다")
    void rejectSubmissionDetailWhenPolicyDeniesTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember actor = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        StudyMember owner = studyMember(assignment, 21L, StudyMemberRole.MEMBER, "제출자");
        AssignmentSubmission submission = submissionWithId(300L, owner, assignment);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(actor);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID))
                .thenReturn(submission);
        doThrow(new AuthException(AuthErrorCode.ACCESS_DENIED))
                .when(assignmentAccessPolicy).requireCanReadSubmission(actor, submission);

        assertAccessDenied(() -> assignmentSubmissionService.getSubmissionDetail(
                USER_ID, STUDY_ID, ASSIGNMENT_ID, 300L));

        verify(assignmentAccessPolicy).requireCanReadSubmission(actor, submission);
    }

    @Test
    @DisplayName("리더가 제출 목록을 조회하면 제출 완료된 제출물만 반환한다")
    void getSubmissionListTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember leader = studyMember(assignment, MEMBER_ID, StudyMemberRole.LEADER, "리더");
        StudyMember submitter = studyMember(assignment, 22L, StudyMemberRole.MEMBER, "제출자");
        AssignmentSubmission submission = submissionWithId(300L, submitter, assignment);
        submission.submit("제출 내용", null, NOW);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findAllByAssignmentIdAndSubmittedTrue(ASSIGNMENT_ID))
                .thenReturn(List.of(submission));

        SubmissionListResponse response = assignmentSubmissionService.getSubmissionList(USER_ID, STUDY_ID,
                ASSIGNMENT_ID);

        assertThat(response.submissions()).singleElement()
                .satisfies(summary -> assertThat(summary.id()).isEqualTo(300L));
        verify(assignmentAccessPolicy).requireCanReadSubmissionList(leader);
        verify(assignmentSubmissionRepository).findAllByAssignmentIdAndSubmittedTrue(ASSIGNMENT_ID);
    }

    @Test
    @DisplayName("제출 목록 조회 정책이 거부하면 저장소 조회를 중단한다")
    void rejectSubmissionListWhenPolicyDeniesTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        doThrow(new AuthException(AuthErrorCode.ACCESS_DENIED))
                .when(assignmentAccessPolicy).requireCanReadSubmissionList(member);

        assertAccessDenied(() -> assignmentSubmissionService.getSubmissionList(USER_ID, STUDY_ID, ASSIGNMENT_ID));

        verify(assignmentAccessPolicy).requireCanReadSubmissionList(member);
        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("리더가 제출 현황을 조회하면 완료 및 미완료 스터디원을 분류한다")
    void getAssignmentSubmissionStatusTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        assignment.addReminders(List.of(NOW.plusDays(1)), NOW);
        StudyMember leader = studyMember(assignment, 21L, StudyMemberRole.LEADER, "리더");
        List<AssignmentSubmitterStatusProjection> statuses = List.of(
                new AssignmentSubmitterStatusProjection(MEMBER_ID, "완료자", "complete.png", true, null),
                new AssignmentSubmitterStatusProjection(22L, "미완료자", "incomplete.png", false,
                        NOW.minusHours(1))
        );
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(assignmentRepository.getByIdAndStudyIdOrThrow(ASSIGNMENT_ID, STUDY_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findAllSubmitterStatusesByAssignmentId(ASSIGNMENT_ID))
                .thenReturn(statuses);

        AssignmentSubmissionStatusResponse response = assignmentSubmissionService.getAssignmentSubmissionStatus(
                USER_ID, STUDY_ID, ASSIGNMENT_ID);

        assertThat(response.memberCount()).isEqualTo(2);
        assertThat(response.completeCount()).isEqualTo(1);
        assertThat(response.incompleteCount()).isEqualTo(1);
        assertThat(response.completeMembers()).extracting(AssignmentSubmissionStatusResponse.CompleteMember::id)
                .containsExactly(MEMBER_ID);
        assertThat(response.incompleteMembers()).singleElement()
                .satisfies(member -> assertThat(member.lastRemindAt()).isEqualTo(NOW.minusHours(1)));
        verify(assignmentAccessPolicy).requireCanReadAssignmentSubmissionStatus(leader);
        verify(assignmentSubmissionRepository).findAllSubmitterStatusesByAssignmentId(ASSIGNMENT_ID);
    }

    @Test
    @DisplayName("제출 현황 조회 정책이 거부하면 저장소 조회를 중단한다")
    void rejectAssignmentSubmissionStatusWhenPolicyDeniesTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        doThrow(new AuthException(AuthErrorCode.ACCESS_DENIED))
                .when(assignmentAccessPolicy).requireCanReadAssignmentSubmissionStatus(member);

        assertAccessDenied(() -> assignmentSubmissionService.getAssignmentSubmissionStatus(USER_ID, STUDY_ID,
                ASSIGNMENT_ID));

        verify(assignmentAccessPolicy).requireCanReadAssignmentSubmissionStatus(member);
        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    private Assignment assignmentWithId(Long assignmentId) {
        return AssignmentTestFixture.assignmentWithId(assignmentId, STUDY_ID, NOW);
    }

    private StudyMember studyMember(Assignment assignment, Long memberId, StudyMemberRole role, String name) {
        StudyMember member = StudyMember.create(assignment.getStudy(), User.create(name, null), name, null, role);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private AssignmentSubmission submissionWithId(Long submissionId, StudyMember member, Assignment assignment) {
        AssignmentSubmission submission = AssignmentSubmission.create(member, assignment);
        ReflectionTestUtils.setField(submission, "id", submissionId);
        return submission;
    }

    private void assertAccessDenied(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);
    }

    private void assertAssignmentNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND);
    }
}
