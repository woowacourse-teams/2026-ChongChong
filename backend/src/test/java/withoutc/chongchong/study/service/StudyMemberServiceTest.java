package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.study.dto.StudyInviteTokenRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
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
    private AssignmentRepository assignmentRepository;

    @Mock
    private StudyInviteTokenProvider studyInviteTokenProvider;

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
        Assignment assignment = mock(Assignment.class);
        when(study.getId()).thenReturn(studyId);
        StudyInviteTokenRequest request = new StudyInviteTokenRequest(token);
        ArgumentCaptor<StudyMember> captor = ArgumentCaptor.forClass(StudyMember.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyInviteTokenProvider.verifyAndExtractStudyId(token)).thenReturn(studyId);
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyMemberRepository.findByStudyIdAndUserId(studyId, userId)).thenReturn(Optional.empty());
        when(studyMemberRepository.countByStudyId(studyId)).thenReturn(1);
        when(assignmentRepository.findAllByStudyId(studyId)).thenReturn(List.of(assignment));

        studyMemberService.join(userId, request);

        verify(studyMemberRepository).save(captor.capture());
        StudyMember studyMember = captor.getValue();
        assertThat(studyMember.getStudy()).isSameAs(study);
        assertThat(studyMember.getUser()).isSameAs(user);
        assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.MEMBER);
        verify(assignment).initializeSubmissions(List.of(studyMember));
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
}
