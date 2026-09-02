package withoutc.chongchong.study.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

class StudyTest {

    @Test
    @DisplayName("스터디 생성 시 설명은 null이어도 허용한다")
    void createWithNullDescriptionTest() {
        Study study = Study.create("자바 스터디", null);

        assertThat(study.getDescription()).isNull();
    }

    @Test
    @DisplayName("스터디 생성 시 이름이 null이면 거부한다")
    void createWithNullNameTest() {
        assertThatThrownBy(() -> Study.create(null, "설명"))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_NAME);
    }

    @Test
    @DisplayName("스터디 설명은 30자까지 허용하고 30자를 초과하면 거부한다")
    void validateDescriptionLengthTest() {
        String maxLengthDescription = "가".repeat(30);
        String overLengthDescription = "가".repeat(31);

        assertThatCode(() -> Study.create("자바 스터디", maxLengthDescription))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> Study.create("자바 스터디", overLengthDescription))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_DESCRIPTION);
    }

    @Test
    @DisplayName("스터디 수정 시 null인 설명은 기존 값을 유지한다")
    void updateWithNullDescriptionTest() {
        Study study = Study.create("기존 스터디", "기존 설명");

        study.update(null, null);

        assertThat(study.getName()).isEqualTo("기존 스터디");
        assertThat(study.getDescription()).isEqualTo("기존 설명");
    }
}
