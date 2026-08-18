package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyInviteLinkGenerator studyInviteLinkGenerator;

    @InjectMocks
    private StudyService studyService;

    @Test
    @DisplayName("스터디 생성 요청으로 Study를 저장한다")
    void createTest() {
        Long userId = 1L;
        StudyCreateRequest request = new StudyCreateRequest("자바 스터디", "매주 월요일에 진행한다.");
        User user = User.create("사용자", "profile-image-url");
        ArgumentCaptor<Study> studyCaptor = ArgumentCaptor.forClass(Study.class);
        ArgumentCaptor<StudyMember> studyMemberCaptor = ArgumentCaptor.forClass(StudyMember.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyRepository.save(any(Study.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        studyService.create(userId, request);

        verify(studyRepository).save(studyCaptor.capture());
        verify(studyMemberRepository).save(studyMemberCaptor.capture());
        Study study = studyCaptor.getValue();
        assertThat(study.getName()).isEqualTo("자바 스터디");
        assertThat(study.getDescription()).isEqualTo("매주 월요일에 진행한다.");

        StudyMember studyMember = studyMemberCaptor.getValue();
        assertThat(studyMember.getStudy()).isSameAs(study);
        assertThat(studyMember.getUser()).isSameAs(user);
        assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.LEADER);
        assertThat(studyMember.getName()).isEqualTo(user.getName());
        assertThat(studyMember.getProfileImageUrl()).isEqualTo(user.getProfileImageUrl());
    }

    @Test
    @DisplayName("존재하는 스터디의 초대 링크를 반환한다")
    void getInviteLinkTest() {
        Long studyId = 1L;
        Study study = Study.create("자바 스터디", "설명");
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyInviteLinkGenerator.generate(studyId))
                .thenReturn("https://test.chongchong.app/join?token=invite-token");

        StudyInviteLinkResponse response = studyService.getInviteLink(studyId);

        assertThat(response.inviteLink()).isEqualTo("https://test.chongchong.app/join?token=invite-token");
        verify(studyInviteLinkGenerator).generate(studyId);
    }

    @Test
    @DisplayName("존재하지 않는 스터디의 초대 링크를 요청하면 예외가 발생한다")
    void getInviteLinkForMissingStudyTest() {
        Long studyId = 1L;
        when(studyRepository.findById(studyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getInviteLink(studyId))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.STUDY_NOT_FOUND);

        verifyNoInteractions(studyInviteLinkGenerator, userRepository, studyMemberRepository);
    }
}
