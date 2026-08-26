package withoutc.chongchong.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
import withoutc.chongchong.assignment.controller.dto.AssignmentSummaryResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
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
        assertThat(response.id()).isEqualTo(ASSIGNMENT_ID);
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
                writer, "과제 제목", "과제 내용", "링크 제출", NOW.plusDays(7), fixedClock()
        );
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        return assignment;
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"), ZoneId.of("Asia/Seoul"));
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
