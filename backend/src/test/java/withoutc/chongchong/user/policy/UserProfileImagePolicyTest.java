package withoutc.chongchong.user.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;

class UserProfileImagePolicyTest {

    private final UserProfileImagePolicy policy = new UserProfileImagePolicy(
            5242880, List.of("image/jpeg", "image/png", "image/webp"), 5242880);

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("홈 이미지 파일 식별자는 필수이며 양수여야 한다")
    void rejectsMissingOrNonPositiveFileId(Long fileId) {
        assertThatThrownBy(() -> policy.validateId(fileId))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_ID));
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, Long.MAX_VALUE})
    @DisplayName("양수 파일 식별자를 허용한다")
    void acceptsPositiveFileId(long fileId) {
        assertThatCode(() -> policy.validateId(fileId)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("JPEG·PNG·WebP 홈 이미지를 허용한다")
    void acceptsSupportedImageType(String contentType) {
        assertThatCode(() -> policy.validateImage(file(contentType, 1024))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "image/gif", "image/svg+xml"})
    @DisplayName("허용하지 않는 홈 이미지 형식을 거절한다")
    void rejectsUnsupportedImageType(String contentType) {
        assertThatThrownBy(() -> policy.validateImage(file(contentType, 1024)))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_TYPE));
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 5242880L})
    @DisplayName("홈 이미지는 1바이트부터 정확히 5MiB까지 허용한다")
    void acceptsImageSizeAtBoundaries(long size) {
        assertThatCode(() -> policy.validateImage(file("image/png", size))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5242881L})
    @DisplayName("0바이트와 5MiB를 한 바이트 초과한 홈 이미지는 거절한다")
    void rejectsImageSizeOutsideBoundaries(long size) {
        assertThatThrownBy(() -> policy.validateImage(file("image/png", size)))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_SIZE));
    }

    @Test
    @DisplayName("설정한 홈 이미지 형식과 크기 제한을 적용한다")
    void appliesConfiguredImageRules() {
        UserProfileImagePolicy configuredPolicy = new UserProfileImagePolicy(1024, List.of("image/png"), 5242880);

        assertThatCode(() -> configuredPolicy.validateImage(file("image/png", 1024))).doesNotThrowAnyException();
        assertThatThrownBy(() -> configuredPolicy.validateImage(file("image/webp", 1024)))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_TYPE));
        assertThatThrownBy(() -> configuredPolicy.validateImage(file("image/png", 1025)))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_SIZE));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    @DisplayName("홈 이미지의 최대 크기를 양수로 설정해야 한다")
    void rejectsInvalidSizeConfiguration(long maxSizeBytes) {
        assertThatThrownBy(() -> new UserProfileImagePolicy(maxSizeBytes, List.of("image/png"), 5242880))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("홈 이미지 허용 형식을 비워 둘 수 없다")
    void rejectsEmptyContentTypesConfiguration() {
        assertThatThrownBy(() -> new UserProfileImagePolicy(5242880, List.of(), 5242880))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("프로필 최대 크기보다 공통 업로드 제한이 작으면 공통 제한을 함께 적용한다")
    void appliesSmallerCommonUploadLimit() {
        UserProfileImagePolicy configuredPolicy = new UserProfileImagePolicy(5242880, List.of("image/png"), 1024);

        assertThatCode(() -> configuredPolicy.validateImage(file("image/png", 1024))).doesNotThrowAnyException();
        assertThatThrownBy(() -> configuredPolicy.validateImage(file("image/png", 1025)))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_SIZE));
    }

    @Test
    @DisplayName("공통 업로드 제한이 0이면 비어 있지 않은 프로필 이미지를 연결할 수 없다")
    void rejectsImageWhenCommonUploadsAreDisabled() {
        UserProfileImagePolicy configuredPolicy = new UserProfileImagePolicy(5242880, List.of("image/png"), 0);

        assertThatThrownBy(() -> configuredPolicy.validateImage(file("image/png", 1)))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_SIZE));
    }

    @Test
    @DisplayName("공통 업로드 제한은 음수로 설정할 수 없다")
    void rejectsNegativeCommonUploadLimit() {
        assertThatThrownBy(() -> new UserProfileImagePolicy(5242880, List.of("image/png"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private FileMetadata file(String contentType, long size) {
        return FileMetadata.create(User.create("소유자", null), "image.png", "uploads/profile-image", contentType, size);
    }
}
