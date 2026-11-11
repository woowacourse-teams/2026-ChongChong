package withoutc.chongchong.study.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.user.entity.User;

class StudyImagePolicyTest {

    private final StudyImagePolicy policy = new StudyImagePolicy(
            5242880, List.of("image/jpeg", "image/png", "image/webp"));

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("JPEG·PNG·WebP 스터디 이미지를 허용한다")
    void acceptsSupportedImageType(String contentType) {
        FileMetadata file = file(contentType, 1024);

        assertThatCode(() -> policy.validateImage(file)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "image/gif", "image/svg+xml"})
    @DisplayName("허용하지 않는 스터디 이미지 형식은 거절한다")
    void rejectsUnsupportedImageType(String contentType) {
        FileMetadata file = file(contentType, 1024);

        assertThatThrownBy(() -> policy.validateImage(file))
                .isInstanceOfSatisfying(StudyException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StudyErrorCode.INVALID_IMAGE_TYPE));
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 5242880L})
    @DisplayName("스터디 이미지는 1바이트부터 정확히 5MiB까지 허용한다")
    void acceptsImageSizeAtBoundaries(long size) {
        FileMetadata file = file("image/png", size);

        assertThatCode(() -> policy.validateImage(file)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5242881L})
    @DisplayName("0바이트와 5MiB를 한 바이트 초과한 스터디 이미지는 거절한다")
    void rejectsImageSizeOutsideBoundaries(long size) {
        FileMetadata file = file("image/png", size);

        assertThatThrownBy(() -> policy.validateImage(file))
                .isInstanceOfSatisfying(StudyException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StudyErrorCode.INVALID_IMAGE_SIZE));
    }

    private FileMetadata file(String contentType, long size) {
        return FileMetadata.create(User.create("소유자", null), "image.png", "study-images/image", contentType, size);
    }
}
