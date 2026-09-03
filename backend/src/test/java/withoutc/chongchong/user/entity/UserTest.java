package withoutc.chongchong.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;

class UserTest {

    @Test
    @DisplayName("사용자는 이름과 프로필 이미지 URL을 보관한다")
    void createUserTest() {
        User user = User.create("총총이", "https://example.com/profile.png");

        assertThat(user.getName()).isEqualTo("총총이");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    @DisplayName("프로필 이미지 URL이 없어도 사용자를 생성할 수 있다")
    void createUserWithNullProfileImageUrlTest() {
        User user = User.create("총총이", null);

        assertThat(user.getProfileImageUrl()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("사용자 이름이 비어 있으면 생성할 수 없다")
    void rejectBlankNameTest(String name) {
        assertThatThrownBy(() -> User.create(name, null))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_USER_NAME);
    }

    @Test
    @DisplayName("사용자 이름은 255자까지 허용하고 255자를 초과하면 거부한다")
    void validateNameLengthTest() {
        String maxLengthName = "가".repeat(255);
        String overLengthName = "가".repeat(256);

        assertThatCode(() -> User.create(maxLengthName, null))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> User.create(overLengthName, null))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_USER_NAME);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("프로필 이미지 URL이 공백이면 사용자를 생성할 수 없다")
    void rejectBlankProfileImageUrlTest(String profileImageUrl) {
        assertThatThrownBy(() -> User.create("총총이", profileImageUrl))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("프로필 이미지 URL은 2048자까지 허용하고 2048자를 초과하면 거부한다")
    void validateProfileImageUrlLengthTest() {
        String maxLengthProfileImageUrl = "a".repeat(2048);
        String overLengthProfileImageUrl = "a".repeat(2049);

        assertThatCode(() -> User.create("총총이", maxLengthProfileImageUrl))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> User.create("총총이", overLengthProfileImageUrl))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_URL);
    }
}
