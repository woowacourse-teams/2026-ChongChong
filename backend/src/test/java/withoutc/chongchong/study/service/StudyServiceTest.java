package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @InjectMocks
    private StudyService studyService;

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
}
