package withoutc.chongchong.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentDetailResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentListResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentStatusesResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSummaryResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

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

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"), zoneId);
        assignmentService = new AssignmentService(
                assignmentRepository,
                assignmentSubmissionRepository,
                studyMemberRepository,
                clock
        );
    }

    @Test
    @DisplayName("리더가 과제를 생성하면 리더를 제외한 스터디원에게 제출 정보를 만들고 리마인더를 등록한다")
    void createTest() {
        Study study = mock(Study.class);
        StudyMember leader = mock(StudyMember.class);
        StudyMember member = mock(StudyMember.class);
        LocalDateTime closeAt = NOW.plusDays(7);
        LocalDateTime remindAt = NOW.plusDays(1);
        AssignmentCreateRequest request = new AssignmentCreateRequest(
                "과제 제목", "과제 내용", "링크 제출", closeAt, List.of(remindAt)
        );
        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(leader.getStudy()).thenReturn(study);
        when(studyMemberRepository.findAllByStudyId(STUDY_ID)).thenReturn(List.of(leader, member));
        when(member.isLeader()).thenReturn(false);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            ReflectionTestUtils.setField(assignment, "id", ASSIGNMENT_ID);
            return assignment;
        });

        AssignmentCreateResponse response = assignmentService.create(USER_ID, STUDY_ID, request);

        verify(assignmentRepository).save(assignmentCaptor.capture());
        Assignment assignment = assignmentCaptor.getValue();
        assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(assignment.getStudy()).isSameAs(study);
        assertThat(assignment.getWriter()).isSameAs(leader);
        assertThat(assignment.getSubmissions()).singleElement()
                .satisfies(submission -> assertThat(submission.getMember()).isSameAs(member));
        assertThat(assignment.getNextRemindAt()).isEqualTo(remindAt);
    }

    @Test
    @DisplayName("리더가 아니면 과제를 생성할 수 없다")
    void createByMemberTest() {
        StudyMember member = mock(StudyMember.class);
        AssignmentCreateRequest request = new AssignmentCreateRequest(
                "과제 제목", "과제 내용", "링크 제출", NOW.plusDays(1), List.of()
        );
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.isLeader()).thenReturn(false);

        assertAccessDenied(() -> assignmentService.create(USER_ID, STUDY_ID, request));

        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("리더가 아니면 과제를 수정할 수 없다")
    void updateByMemberTest() {
        StudyMember member = mock(StudyMember.class);
        AssignmentUpdateRequest request = new AssignmentUpdateRequest("수정 제목", null, null, null, null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.isLeader()).thenReturn(false);

        assertAccessDenied(() -> assignmentService.update(USER_ID, STUDY_ID, ASSIGNMENT_ID, request));

        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("리더가 아니면 과제를 삭제할 수 없다")
    void deleteByMemberTest() {
        StudyMember member = mock(StudyMember.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.isLeader()).thenReturn(false);

        assertAccessDenied(() -> assignmentService.delete(USER_ID, STUDY_ID, ASSIGNMENT_ID));

        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("과제를 수정하면 변경된 애그리거트 루트를 저장한다")
    void updateTest() {
        StudyMember leader = mock(StudyMember.class);
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        LocalDateTime closeAt = NOW.plusDays(10);
        AssignmentUpdateRequest request = new AssignmentUpdateRequest(
                "수정 제목", "수정 내용", "파일 제출", closeAt, List.of(NOW.plusDays(2))
        );
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);

        assignmentService.update(USER_ID, STUDY_ID, ASSIGNMENT_ID, request);

        assertThat(assignment.getTitle()).isEqualTo("수정 제목");
        assertThat(assignment.getContent()).isEqualTo("수정 내용");
        assertThat(assignment.getSubmissionMethod()).isEqualTo("파일 제출");
        assertThat(assignment.getCloseAt()).isEqualTo(closeAt);
        assertThat(assignment.getNextRemindAt()).isEqualTo(NOW.plusDays(2));
        verify(assignmentRepository).save(assignment);
    }

    @Test
    @DisplayName("다른 스터디의 과제는 수정할 수 없다")
    void updateAssignmentFromOtherStudyTest() {
        StudyMember leader = mock(StudyMember.class);
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID, 999L);
        AssignmentUpdateRequest request = new AssignmentUpdateRequest("수정 제목", null, null, null, null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);

        assertAssignmentNotFound(() -> assignmentService.update(USER_ID, STUDY_ID, ASSIGNMENT_ID, request));

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    @DisplayName("과제 삭제 시 소속 스터디를 확인하고 애그리거트 루트를 삭제한다")
    void deleteTest() {
        StudyMember leader = mock(StudyMember.class);
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);

        assignmentService.delete(USER_ID, STUDY_ID, ASSIGNMENT_ID);

        verify(assignmentRepository).delete(assignment);
    }

    @Test
    @DisplayName("다른 스터디의 과제는 삭제할 수 없다")
    void deleteAssignmentFromOtherStudyTest() {
        StudyMember leader = mock(StudyMember.class);
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID, 999L);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);

        assertAssignmentNotFound(() -> assignmentService.delete(USER_ID, STUDY_ID, ASSIGNMENT_ID));

        verify(assignmentRepository, never()).delete(any(Assignment.class));
    }

    @Test
    @DisplayName("스터디 참여자가 소속 스터디의 과제 상세 정보를 조회한다")
    void getDetailTest() {
        StudyMember member = mock(StudyMember.class);
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);

        AssignmentDetailResponse response = assignmentService.getDetail(USER_ID, STUDY_ID, ASSIGNMENT_ID);

        assertThat(response.id()).isEqualTo(ASSIGNMENT_ID);
        assertThat(response.title()).isEqualTo("과제 제목");
        assertThat(response.content()).isEqualTo("과제 내용");
        assertThat(response.submissionMethod()).isEqualTo("링크 제출");
        assertThat(response.closeAt()).isEqualTo(NOW.plusDays(7));
    }

    @Test
    @DisplayName("스터디에 참여하지 않은 사용자는 과제 상세 정보를 조회할 수 없다")
    void getDetailByNonParticipantTest() {
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID))
                .thenThrow(new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));

        assertThatThrownBy(() -> assignmentService.getDetail(USER_ID, STUDY_ID, ASSIGNMENT_ID))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_ACCESS_DENIED);
        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("다른 스터디의 과제 상세 정보는 조회할 수 없다")
    void getDetailFromOtherStudyTest() {
        StudyMember member = mock(StudyMember.class);
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID, 999L);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);

        assertAssignmentNotFound(() -> assignmentService.getDetail(USER_ID, STUDY_ID, ASSIGNMENT_ID));
    }

    @Test
    @DisplayName("리더가 과제 목록을 조회하면 제출 인원과 완료 인원을 반환하고 다음 cursor를 계산한다")
    void getListForLeaderTest() {
        StudyMember leader = mock(StudyMember.class);
        Assignment firstAssignment = assignmentSummaryMock(300L, 3, 2);
        Assignment secondAssignment = assignmentSummaryMock(200L, 2, 2);
        Assignment nextPageAssignment = mock(Assignment.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(assignmentRepository.findByCursor(STUDY_ID, null, PageRequest.of(0, 3)))
                .thenReturn(List.of(firstAssignment, secondAssignment, nextPageAssignment));

        AssignmentListResponse response = assignmentService.getList(USER_ID, STUDY_ID, null, 2);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(200L);
        assertThat(response.assignments()).extracting(AssignmentSummaryResponse::id)
                .containsExactly(300L, 200L);
        assertThat(response.assignments().getFirst().memberCount()).isEqualTo(3);
        assertThat(response.assignments().getFirst().completeCount()).isEqualTo(2);
        assertThat(response.assignments().getFirst().isComplete()).isFalse();
        assertThat(response.assignments().getLast().isComplete()).isTrue();
        verifyNoInteractions(assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("스터디원이 과제 목록을 조회하면 제출 상태를 한 번에 조회한다")
    void getListForMemberTest() {
        StudyMember member = mock(StudyMember.class);
        Assignment firstAssignment = assignmentWithId(ASSIGNMENT_ID);
        Assignment secondAssignment = assignmentWithId(200L);
        List<AssignmentSubmissionStatusProjection> statuses = List.of(
                new AssignmentSubmissionStatusProjection(ASSIGNMENT_ID, true),
                new AssignmentSubmissionStatusProjection(200L, false)
        );
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(assignmentRepository.findByCursorAndMemberId(
                STUDY_ID, MEMBER_ID, null, PageRequest.of(0, 11)
        ))
                .thenReturn(List.of(firstAssignment, secondAssignment));
        when(assignmentSubmissionRepository.findMySubmissionStatusesByAssignmentIdsAndMemberId(
                List.of(ASSIGNMENT_ID, 200L), MEMBER_ID
        )).thenReturn(statuses);

        AssignmentListResponse response = assignmentService.getList(USER_ID, STUDY_ID, null, 10);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.assignments()).hasSize(2);
        assertThat(response.assignments().getFirst().memberCount()).isNull();
        assertThat(response.assignments().getFirst().completeCount()).isNull();
        assertThat(response.assignments().getFirst().remindAt()).isNull();
        assertThat(response.assignments().getFirst().isComplete()).isTrue();
        assertThat(response.assignments().getLast().isComplete()).isFalse();
        verify(assignmentSubmissionRepository).findMySubmissionStatusesByAssignmentIdsAndMemberId(
                List.of(ASSIGNMENT_ID, 200L), MEMBER_ID
        );
    }

    @Test
    @DisplayName("스터디원의 제출 정보가 없는 과제는 목록에서 제외한다")
    void getListWithoutSubmissionTest() {
        StudyMember member = mock(StudyMember.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(assignmentRepository.findByCursorAndMemberId(
                STUDY_ID, MEMBER_ID, null, PageRequest.of(0, 11)
        )).thenReturn(List.of());

        AssignmentListResponse response = assignmentService.getList(USER_ID, STUDY_ID, null, 10);

        assertThat(response.assignments()).isEmpty();
        verifyNoInteractions(assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("스터디원이 자신의 과제를 제출하면 해당 스터디원 제출물만 상태를 변경한다")
    void submitAssignmentTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        AssignmentSubmitRequest request = new AssignmentSubmitRequest("제출 내용", "https://example.com");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByAssignmentIdAndMemberIdOrThrow(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(submission);

        AssignmentSubmitResponse response = assignmentService.submitAssignment(USER_ID, STUDY_ID, ASSIGNMENT_ID,
                request);

        assertThat(response.submissionId()).isEqualTo(300L);
        assertThat(submission.isSubmitted()).isTrue();
        assertThat(submission.getContent()).isEqualTo("제출 내용");
        assertThat(submission.getLink()).isEqualTo("https://example.com");
        assertThat(submission.getSubmittedAt()).isEqualTo(NOW);
        verify(assignmentSubmissionRepository).getByAssignmentIdAndMemberIdOrThrow(ASSIGNMENT_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("URL의 스터디와 과제의 스터디가 다르면 과제를 제출할 수 없다")
    void submitAssignmentFromOtherStudyTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID, 999L);
        Assignment currentStudyAssignment = assignmentWithId(200L);
        StudyMember member = studyMember(currentStudyAssignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmitRequest request = new AssignmentSubmitRequest("제출 내용", null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);

        assertAssignmentNotFound(
                () -> assignmentService.submitAssignment(USER_ID, STUDY_ID, ASSIGNMENT_ID, request)
        );

        verifyNoInteractions(assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("스터디원이 제출물을 수정하면 URL의 과제와 자신의 제출물이 일치할 때만 저장한다")
    void updateSubmissionTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        submission.submit("기존 내용", "https://old.example.com", NOW);
        AssignmentSubmitRequest request = new AssignmentSubmitRequest("수정 내용", null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByIdAndAssignmentIdAndMemberIdOrThrow(300L, ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(submission);

        assignmentService.updateSubmission(USER_ID, STUDY_ID, ASSIGNMENT_ID, 300L, request);

        assertThat(submission.getContent()).isEqualTo("수정 내용");
        assertThat(submission.getLink()).isEqualTo("https://old.example.com");
        verify(assignmentSubmissionRepository).getByIdAndAssignmentIdAndMemberIdOrThrow(300L, ASSIGNMENT_ID,
                MEMBER_ID);
        verify(assignmentSubmissionRepository).save(submission);
    }

    @Test
    @DisplayName("스터디원은 자신이 제출한 과제 제출 정보를 조회한다")
    void getMySubmissionDetailTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        submission.submit("제출 내용", "https://example.com", NOW);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(Optional.of(submission));

        MySubmissionDetailResponse response = assignmentService.getMySubmissionDetail(
                USER_ID, STUDY_ID, ASSIGNMENT_ID
        );

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
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(Optional.of(submission));

        MySubmissionDetailResponse response = assignmentService.getMySubmissionDetail(
                USER_ID, STUDY_ID, ASSIGNMENT_ID
        );

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
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(Optional.empty());

        MySubmissionDetailResponse response = assignmentService.getMySubmissionDetail(
                USER_ID, STUDY_ID, ASSIGNMENT_ID
        );

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("스터디원은 URL의 과제와 자신의 제출물이 일치할 때 제출물 상세를 조회한다")
    void getSubmissionDetailForMemberTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, member, assignment);
        submission.submit("제출 내용", "https://example.com", NOW);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByIdAndAssignmentIdAndMemberIdOrThrow(300L, ASSIGNMENT_ID, MEMBER_ID))
                .thenReturn(submission);

        SubmissionDetailResponse response = assignmentService.getSubmissionDetail(USER_ID, STUDY_ID, ASSIGNMENT_ID,
                300L);

        assertThat(response.id()).isEqualTo(300L);
        assertThat(response.name()).isEqualTo("스터디원");
        assertThat(response.content()).isEqualTo("제출 내용");
        verify(assignmentSubmissionRepository).getByIdAndAssignmentIdAndMemberIdOrThrow(300L, ASSIGNMENT_ID,
                MEMBER_ID);
        verify(assignmentSubmissionRepository, never()).getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID);
    }

    @Test
    @DisplayName("리더는 URL의 과제에 속한 다른 스터디원의 제출물 상세를 조회한다")
    void getSubmissionDetailForLeaderTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember leader = studyMember(assignment, 21L, StudyMemberRole.LEADER, "리더");
        StudyMember submitter = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        AssignmentSubmission submission = submissionWithId(300L, submitter, assignment);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID))
                .thenReturn(submission);

        SubmissionDetailResponse response = assignmentService.getSubmissionDetail(USER_ID, STUDY_ID, ASSIGNMENT_ID,
                300L);

        assertThat(response.name()).isEqualTo("스터디원");
        verify(assignmentSubmissionRepository).getByIdAndAssignmentIdOrThrow(300L, ASSIGNMENT_ID);
        verify(assignmentSubmissionRepository, never()).getByIdAndAssignmentIdAndMemberIdOrThrow(300L,
                ASSIGNMENT_ID, 21L);
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
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findAllByAssignmentIdAndSubmittedTrue(ASSIGNMENT_ID))
                .thenReturn(List.of(submission));

        SubmissionListResponse response = assignmentService.getSubmissionList(USER_ID, STUDY_ID, ASSIGNMENT_ID);

        assertThat(response.submissions()).singleElement()
                .satisfies(summary -> assertThat(summary.id()).isEqualTo(300L));
        verify(studyMemberRepository).getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID);
        verifyNoMoreInteractions(studyMemberRepository);
        verify(assignmentSubmissionRepository).findAllByAssignmentIdAndSubmittedTrue(ASSIGNMENT_ID);
    }

    @Test
    @DisplayName("스터디원은 제출 목록을 조회할 수 없다")
    void rejectSubmissionListForMemberTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);

        assertAccessDenied(() -> assignmentService.getSubmissionList(USER_ID, STUDY_ID, ASSIGNMENT_ID));

        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("리더가 제출 현황을 조회하면 완료 및 미완료 스터디원을 분류한다")
    void getAllSubmittedStatusTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        assignment.addReminders(List.of(NOW.plusDays(1)), NOW);
        StudyMember leader = studyMember(assignment, 21L, StudyMemberRole.LEADER, "리더");
        List<AssignmentSubmitterStatusProjection> statuses = List.of(
                new AssignmentSubmitterStatusProjection(MEMBER_ID, "완료자", "complete.png", true, null),
                new AssignmentSubmitterStatusProjection(22L, "미완료자", "incomplete.png", false,
                        NOW.minusHours(1))
        );
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(assignmentRepository.getByIdOrThrow(ASSIGNMENT_ID)).thenReturn(assignment);
        when(assignmentSubmissionRepository.findAllSubmitterStatusesByAssignmentId(ASSIGNMENT_ID))
                .thenReturn(statuses);

        AssignmentStatusesResponse response = assignmentService.getAllSubmittedStatus(USER_ID, STUDY_ID,
                ASSIGNMENT_ID);

        assertThat(response.memberCount()).isEqualTo(2);
        assertThat(response.completeCount()).isEqualTo(1);
        assertThat(response.incompleteCount()).isEqualTo(1);
        assertThat(response.completeMembers()).extracting(AssignmentStatusesResponse.CompleteMember::id)
                .containsExactly(MEMBER_ID);
        assertThat(response.incompleteMembers()).singleElement()
                .satisfies(member -> assertThat(member.lastRemindAt()).isEqualTo(NOW.minusHours(1)));
        verify(assignmentSubmissionRepository).findAllSubmitterStatusesByAssignmentId(ASSIGNMENT_ID);
    }

    @Test
    @DisplayName("스터디원은 제출 현황을 조회할 수 없다")
    void rejectSubmittedStatusForMemberTest() {
        Assignment assignment = assignmentWithId(ASSIGNMENT_ID);
        StudyMember member = studyMember(assignment, MEMBER_ID, StudyMemberRole.MEMBER, "스터디원");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);

        assertAccessDenied(() -> assignmentService.getAllSubmittedStatus(USER_ID, STUDY_ID, ASSIGNMENT_ID));

        verifyNoInteractions(assignmentRepository, assignmentSubmissionRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {101, Integer.MAX_VALUE})
    @DisplayName("페이지 크기가 최대값을 초과하면 저장소를 조회하지 않는다")
    void rejectPageSizeOverMaximumTest(int size) {
        assertThatThrownBy(() -> assignmentService.getList(USER_ID, STUDY_ID, null, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("페이지 크기는 1 이상 100 이하여야 합니다.");

        verifyNoInteractions(studyMemberRepository, assignmentRepository, assignmentSubmissionRepository);
    }

    private Assignment assignmentSummaryMock(Long assignmentId, int memberCount, int completeCount) {
        Assignment assignment = mock(Assignment.class);
        when(assignment.getId()).thenReturn(assignmentId);
        when(assignment.getTitle()).thenReturn("과제 제목");
        when(assignment.getContent()).thenReturn("과제 내용");
        when(assignment.getSubmissionMethod()).thenReturn("링크 제출");
        when(assignment.getCloseAt()).thenReturn(NOW.plusDays(7));
        when(assignment.getSubmissionCount()).thenReturn(memberCount);
        when(assignment.getSubmittedCount()).thenReturn(completeCount);
        return assignment;
    }

    private Assignment assignmentWithId(Long assignmentId) {
        return assignmentWithId(assignmentId, STUDY_ID);
    }

    private Assignment assignmentWithId(Long assignmentId, Long studyId) {
        Study study = Study.create("자바 스터디", "설명");
        User user = User.create("리더", null);
        StudyMember writer = StudyMember.create(study, user, "리더", null, StudyMemberRole.LEADER);
        ReflectionTestUtils.setField(study, "id", studyId);
        Assignment assignment = Assignment.create(
                writer, "과제 제목", "과제 내용", "링크 제출", NOW.plusDays(7), NOW
        );
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        return assignment;
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
