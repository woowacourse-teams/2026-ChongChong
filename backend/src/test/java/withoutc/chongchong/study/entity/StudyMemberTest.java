package withoutc.chongchong.study.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.user.entity.User;

class StudyMemberTest {

    private final Study study = mock(Study.class);
    private final User user = mock(User.class);

    @Test
    @DisplayName("스터디 리더의 역할을 확인한다")
    void isLeaderTest() {
        StudyMember member = createMember(StudyMemberRole.LEADER);

        assertThat(member.isLeader()).isTrue();
    }

    @Test
    @DisplayName("일반 스터디원은 리더가 아님을 확인한다")
    void isNotLeaderTest() {
        StudyMember member = createMember(StudyMemberRole.MEMBER);

        assertThat(member.isLeader()).isFalse();
    }

    @Test
    @DisplayName("스터디가 없으면 스터디 멤버를 생성할 수 없다")
    void rejectNullStudyTest() {
        assertInvalidRequiredValue(null, user, StudyMemberRole.MEMBER);
    }

    @Test
    @DisplayName("사용자가 없으면 스터디 멤버를 생성할 수 없다")
    void rejectNullUserTest() {
        assertInvalidRequiredValue(study, null, StudyMemberRole.MEMBER);
    }

    @Test
    @DisplayName("역할이 없으면 스터디 멤버를 생성할 수 없다")
    void rejectNullRoleTest() {
        assertInvalidRequiredValue(study, user, null);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("스터디 멤버 이름이 비어 있으면 생성할 수 없다")
    void rejectBlankNameTest(String name) {
        assertThatThrownBy(() -> StudyMember.create(
                study,
                user,
                name,
                null,
                StudyMemberRole.MEMBER
        ))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.INVALID_STUDY_MEMBER_NAME);
    }

    @Test
    @DisplayName("스터디 멤버 이름은 255자까지 허용하고 255자를 초과하면 거부한다")
    void validateNameLengthTest() {
        String maxLengthName = "가".repeat(255);
        String overLengthName = "가".repeat(256);

        assertThatCode(() -> createMember(maxLengthName, null))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> createMember(overLengthName, null))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.INVALID_STUDY_MEMBER_NAME);
    }

    @Test
    @DisplayName("프로필 이미지 URL이 없어도 스터디 멤버를 생성할 수 있다")
    void createMemberWithNullProfileImageUrlTest() {
        StudyMember member = createMember("스터디원", null);

        assertThat(member.getProfileImageUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("프로필 이미지 URL이 공백이면 스터디 멤버를 생성할 수 없다")
    void rejectBlankProfileImageUrlTest(String profileImageUrl) {
        assertThatThrownBy(() -> createMember("스터디원", profileImageUrl))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.INVALID_STUDY_MEMBER_PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("프로필 이미지 URL은 2048자까지 허용하고 2048자를 초과하면 거부한다")
    void validateProfileImageUrlLengthTest() {
        String maxLengthProfileImageUrl = "a".repeat(2048);
        String overLengthProfileImageUrl = "a".repeat(2049);

        assertThatCode(() -> createMember("스터디원", maxLengthProfileImageUrl))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> createMember("스터디원", overLengthProfileImageUrl))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.INVALID_STUDY_MEMBER_PROFILE_IMAGE_URL);
    }

    private StudyMember createMember(StudyMemberRole role) {
        return StudyMember.create(study, user, "스터디원", "https://example.com/profile.png", role);
    }

    private StudyMember createMember(String name, String profileImageUrl) {
        return StudyMember.create(study, user, name, profileImageUrl, StudyMemberRole.MEMBER);
    }

    private void assertInvalidRequiredValue(Study study, User user, StudyMemberRole role) {
        assertThatThrownBy(() -> StudyMember.create(
                study,
                user,
                "스터디원",
                null,
                role
        ))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.INVALID_STUDY_MEMBER);
    }
}
