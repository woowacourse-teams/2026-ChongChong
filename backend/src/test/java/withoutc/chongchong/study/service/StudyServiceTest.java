package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyInviteTokenProvider studyInviteTokenProvider;

    @InjectMocks
    private StudyService studyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(studyService, "frontendBaseUrl", "https://chongchong.app");
    }

    @Test
    @DisplayName("스터디 생성 요청으로 Study를 저장한다")
    void createTest() {
        StudyCreateRequest request = new StudyCreateRequest("자바 스터디", "매주 월요일에 진행한다.");
        ArgumentCaptor<Study> studyCaptor = ArgumentCaptor.forClass(Study.class);

        studyService.create(request);

        verify(studyRepository).save(studyCaptor.capture());
        Study study = studyCaptor.getValue();
        assertThat(study.getName()).isEqualTo("자바 스터디");
        assertThat(study.getDescription()).isEqualTo("매주 월요일에 진행한다.");
    }

    @Test
    @DisplayName("존재하는 스터디의 초대 링크를 반환한다")
    void getInviteLinkTest() {
        Long studyId = 1L;
        Study study = Study.create("자바 스터디", "설명");
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyInviteTokenProvider.generate(studyId)).thenReturn("invite-token");

        String inviteLink = studyService.getInviteLink(studyId);

        assertThat(inviteLink).isEqualTo("https://chongchong.app/join?token=invite-token");
        verify(studyInviteTokenProvider).generate(studyId);
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

        verifyNoInteractions(studyInviteTokenProvider);
    }
}
