package withoutc.chongchong.study.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

class StudyRepositoryTest {

    private final StudyRepository studyRepository = mock(StudyRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("스터디 조회에 성공하면 해당 스터디를 반환한다")
    void getByIdOrThrowTest() {
        Study study = Study.create("스터디", "설명");
        when(studyRepository.findById(1L)).thenReturn(Optional.of(study));

        Study found = studyRepository.getByIdOrThrow(1L);

        assertThat(found).isSameAs(study);
    }

    @Test
    @DisplayName("스터디가 존재하지 않으면 스터디 없음 예외를 던진다")
    void getByIdOrThrowNotFoundTest() {
        when(studyRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> studyRepository.getByIdOrThrow(Long.MAX_VALUE))
                .isInstanceOfSatisfying(StudyException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StudyErrorCode.STUDY_NOT_FOUND)
                );
    }
}
