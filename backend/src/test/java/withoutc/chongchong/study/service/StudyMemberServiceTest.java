package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.study.dto.StudyInviteTokenRequest;
import withoutc.chongchong.study.dto.StudyMemberResponse;
import withoutc.chongchong.study.dto.StudyMembersResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StudyMemberServiceTest {

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyInviteTokenProvider studyInviteTokenProvider;

    @Mock
    private StudyMemberRemover studyMemberRemover;

    @InjectMocks
    private StudyMemberService studyMemberService;

    @Test
    @DisplayName("유효한 초대 토큰으로 스터디에 참여하고 멤버를 생성한다")
    void joinTest() {
        Long userId = 1L;
        Long studyId = 2L;
        String token = "invite-token";
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn("사용자");
        when(user.getProfileImageUrl()).thenReturn("profile-image-url");
        Study study = mock(Study.class);
        when(study.getId()).thenReturn(studyId);
        StudyInviteTokenRequest request = new StudyInviteTokenRequest(token);
        ArgumentCaptor<StudyMember> captor = ArgumentCaptor.forClass(StudyMember.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyInviteTokenProvider.verifyAndExtractStudyId(token)).thenReturn(studyId);
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyMemberRepository.findByStudyIdAndUserId(studyId, userId)).thenReturn(Optional.empty());
        when(studyMemberRepository.countByStudyId(studyId)).thenReturn(1);
        studyMemberService.join(userId, request);

        verify(studyMemberRepository).save(captor.capture());
        StudyMember studyMember = captor.getValue();
        assertThat(studyMember.getStudy()).isSameAs(study);
        assertThat(studyMember.getUser()).isSameAs(user);
        assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.MEMBER);
    }

    @Test
    @DisplayName("이미 가입한 스터디에는 참여할 수 없다")
    void joinAlreadyJoinedStudyTest() {
        Long userId = 1L;
        Long studyId = 2L;
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        Study study = mock(Study.class);
        when(study.getId()).thenReturn(studyId);
        StudyMember member = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.MEMBER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyInviteTokenProvider.verifyAndExtractStudyId("invite-token")).thenReturn(studyId);
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyMemberRepository.findByStudyIdAndUserId(studyId, userId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> studyMemberService.join(userId, new StudyInviteTokenRequest("invite-token")))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.ALREADY_JOINED_STUDY);

        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("스터디 정원이 가득 차면 참여할 수 없다")
    void joinFullStudyTest() {
        Long userId = 1L;
        Long studyId = 2L;
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        Study study = mock(Study.class);
        when(study.getId()).thenReturn(studyId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyInviteTokenProvider.verifyAndExtractStudyId("invite-token")).thenReturn(studyId);
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyMemberRepository.findByStudyIdAndUserId(studyId, userId)).thenReturn(Optional.empty());
        when(studyMemberRepository.countByStudyId(studyId)).thenReturn(30);

        assertThatThrownBy(() -> studyMemberService.join(userId, new StudyInviteTokenRequest("invite-token")))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);

        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("유효하지 않은 초대 토큰이면 참여할 수 없다")
    void joinInvalidInviteTokenTest() {
        Long userId = 1L;
        User user = User.create("사용자", "profile-image-url");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyInviteTokenProvider.verifyAndExtractStudyId("invalid-token"))
                .thenThrow(new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN));

        assertThatThrownBy(() -> studyMemberService.join(userId, new StudyInviteTokenRequest("invalid-token")))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_INVITE_TOKEN);

        verify(studyRepository, never()).findById(any());
        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("스터디 멤버가 멤버 목록을 조회한다")
    void getStudyMembersTest() {
        Long userId = 1L;
        Long studyId = 2L;
        Study study = mock(Study.class);
        StudyMember requester = mock(StudyMember.class);

        StudyMemberSummaryProjection projection1 = new StudyMemberSummaryProjection(
                10L,
                "리더",
                "leader-profile-image-url",
                StudyMemberRole.LEADER
        );
        StudyMemberSummaryProjection projection2 = new StudyMemberSummaryProjection(
                11L,
                "이든",
                null,
                StudyMemberRole.MEMBER
        );
        List<StudyMemberSummaryProjection> projections = List.of(projection1, projection2);

        when(studyRepository.getByIdOrThrow(studyId)).thenReturn(study);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId)).thenReturn(requester);
        when(studyMemberRepository.findAllSummariesByStudyId(studyId)).thenReturn(projections);

        StudyMembersResponse response = studyMemberService.getAllStudyMembers(userId, studyId);

        assertThat(response.members())
                .containsExactly(StudyMemberResponse.from(projection1), StudyMemberResponse.from(projection2));
    }

    @Test
    @DisplayName("존재하지 않는 스터디의 멤버 목록을 조회할 수 없다")
    void getAllStudyMembersWithMissingStudyTest() {
        Long userId = 1L;
        Long studyId = 999L;

        when(studyRepository.getByIdOrThrow(studyId))
                .thenThrow(new StudyException(
                        StudyErrorCode.STUDY_NOT_FOUND
                ));

        assertThatThrownBy(() -> studyMemberService.getAllStudyMembers(userId, studyId))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.STUDY_NOT_FOUND);

        verifyNoInteractions(studyMemberRepository);
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 멤버 목록을 조회할 수 없다")
    void getAllStudyMembersByNonMemberTest() {
        Long userId = 1L;
        Long studyId = 2L;
        Study study = mock(Study.class);

        when(studyRepository.getByIdOrThrow(studyId)).thenReturn(study);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId))
                .thenThrow(new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));

        assertThatThrownBy(() -> studyMemberService.getAllStudyMembers(userId, studyId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_ACCESS_DENIED);

        verify(studyMemberRepository, never()).findAllSummariesByStudyId(studyId);
    }

    @Test
    @DisplayName("스터디 리더가 같은 스터디의 일반 멤버를 방출한다")
    void expelTest() {
        Long userId = 1L;
        Long studyId = 2L;
        Long memberId = 3L;
        StudyMember leader = mock(StudyMember.class);
        StudyMember target = mock(StudyMember.class);
        when(leader.isLeader()).thenReturn(true);
        when(target.isLeader()).thenReturn(false);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId)).thenReturn(leader);
        when(studyMemberRepository.getByStudyIdAndIdOrThrow(studyId, memberId)).thenReturn(target);

        studyMemberService.expel(userId, studyId, memberId);

        verify(studyMemberRemover).remove(target);
    }

    @Test
    @DisplayName("스터디 멤버가 아닌 사용자는 멤버를 방출할 수 없다")
    void expelByNonMemberTest() {
        Long userId = 1L;
        Long studyId = 2L;
        Long memberId = 3L;
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId))
                .thenThrow(new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));

        assertThatThrownBy(() -> studyMemberService.expel(userId, studyId, memberId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_ACCESS_DENIED);

        verify(studyMemberRepository, never()).getByStudyIdAndIdOrThrow(studyId, memberId);
        verifyNoInteractions(studyMemberRemover);
    }

    @Test
    @DisplayName("스터디 리더가 아닌 멤버는 다른 멤버를 방출할 수 없다")
    void expelByNonLeaderTest() {
        Long userId = 1L;
        Long studyId = 2L;
        Long memberId = 3L;
        StudyMember requester = mock(StudyMember.class);
        when(requester.isLeader()).thenReturn(false);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId)).thenReturn(requester);

        assertThatThrownBy(() -> studyMemberService.expel(userId, studyId, memberId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.NOT_STUDY_LEADER);

        verify(studyMemberRepository, never()).getByStudyIdAndIdOrThrow(studyId, memberId);
        verifyNoInteractions(studyMemberRemover);
    }

    @Test
    @DisplayName("같은 스터디에 대상 멤버가 없으면 방출할 수 없다")
    void expelMissingMemberTest() {
        Long userId = 1L;
        Long studyId = 2L;
        Long memberId = 3L;
        StudyMember leader = mock(StudyMember.class);
        when(leader.isLeader()).thenReturn(true);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId)).thenReturn(leader);
        when(studyMemberRepository.getByStudyIdAndIdOrThrow(studyId, memberId))
                .thenThrow(new StudyMemberException(StudyMemberErrorCode.STUDY_MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> studyMemberService.expel(userId, studyId, memberId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_MEMBER_NOT_FOUND);

        verifyNoInteractions(studyMemberRemover);
    }

    @Test
    @DisplayName("스터디 리더는 방출할 수 없다")
    void expelLeaderTest() {
        Long userId = 1L;
        Long studyId = 2L;
        Long memberId = 3L;
        StudyMember requester = mock(StudyMember.class);
        StudyMember targetLeader = mock(StudyMember.class);
        when(requester.isLeader()).thenReturn(true);
        when(targetLeader.isLeader()).thenReturn(true);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId)).thenReturn(requester);
        when(studyMemberRepository.getByStudyIdAndIdOrThrow(studyId, memberId)).thenReturn(targetLeader);

        assertThatThrownBy(() -> studyMemberService.expel(userId, studyId, memberId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_LEADER_CANNOT_BE_REMOVED);

        verifyNoInteractions(studyMemberRemover);
    }

    @Test
    @DisplayName("일반 멤버가 스터디에서 탈퇴한다")
    void leaveTest() {
        Long userId = 1L;
        Long studyId = 2L;
        StudyMember member = mock(StudyMember.class);
        when(member.isLeader()).thenReturn(false);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId)).thenReturn(member);

        studyMemberService.leave(userId, studyId);

        verify(studyMemberRemover).remove(member);
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 탈퇴할 수 없다")
    void leaveByNonMemberTest() {
        Long userId = 1L;
        Long studyId = 2L;
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId))
                .thenThrow(new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));

        assertThatThrownBy(() -> studyMemberService.leave(userId, studyId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_ACCESS_DENIED);

        verifyNoInteractions(studyMemberRemover);
    }

    @Test
    @DisplayName("스터디 리더는 탈퇴할 수 없다")
    void leaveByLeaderTest() {
        Long userId = 1L;
        Long studyId = 2L;
        StudyMember leader = mock(StudyMember.class);
        when(leader.isLeader()).thenReturn(true);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId)).thenReturn(leader);

        assertThatThrownBy(() -> studyMemberService.leave(userId, studyId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_LEADER_CANNOT_LEAVE);

        verifyNoInteractions(studyMemberRemover);
    }
}
