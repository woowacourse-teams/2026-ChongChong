package withoutc.chongchong.study.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class StudyRepositoryTest {

    @Autowired
    private StudyRepository studyRepository;

    @Test
    @DisplayName("스터디 조회에 성공하면 해당 스터디를 반환한다")
    void getByIdOrThrowTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));

        Study found = studyRepository.getByIdOrThrow(study.getId());

        assertThat(found.getId()).isEqualTo(study.getId());
    }

    @Test
    @DisplayName("스터디가 존재하지 않으면 스터디 없음 예외를 던진다")
    void getByIdOrThrowNotFoundTest() {
        assertThatThrownBy(() -> studyRepository.getByIdOrThrow(Long.MAX_VALUE))
                .isInstanceOfSatisfying(StudyException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StudyErrorCode.STUDY_NOT_FOUND)
                );
    }
}
