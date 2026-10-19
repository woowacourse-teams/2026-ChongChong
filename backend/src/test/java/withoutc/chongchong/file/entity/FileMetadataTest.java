package withoutc.chongchong.file.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.user.entity.User;

class FileMetadataTest {

    private final User owner = User.create("사용자", null);

    @Test
    @DisplayName("소유자가 없으면 파일을 생성할 수 없다")
    void rejectNullOwner() {
        assertThatThrownBy(() -> FileMetadata.create(null, "a.pdf", "uploads/a.pdf", "application/pdf", 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_FILE_OWNER);
    }

    @Test
    @DisplayName("음수 크기의 파일을 생성할 수 없다")
    void rejectNegativeSize() {
        assertThatThrownBy(() -> FileMetadata.create(owner, "a.pdf", "uploads/a.pdf", "application/pdf", -1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_FILE_SIZE);
    }

    @Test
    @DisplayName("최대 길이와 0바이트를 허용하고 업로드 대기 상태로 생성한다")
    void createAtBoundaries() {
        FileMetadata file = FileMetadata.create(owner, "가".repeat(255), "a".repeat(1024), "a".repeat(255), 0);

        assertThat(file.getOwner()).isSameAs(owner);
        assertThat(file.getOriginalFileName()).hasSize(255);
        assertThat(file.getObjectKey()).hasSize(1024);
        assertThat(file.getContentType()).hasSize(255);
        assertThat(file.getSize()).isZero();
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.PENDING);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("원본 파일명이 없거나 공백이면 거부한다")
    void rejectBlankOriginalFileName(String value) {
        assertThatThrownBy(() -> FileMetadata.create(owner, value, "uploads/a.pdf", "application/pdf", 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_ORIGINAL_FILE_NAME);
    }

    @Test
    @DisplayName("원본 파일명이 최대 길이를 초과하면 거부한다")
    void rejectLongOriginalFileName() {
        String value = "a".repeat(256);

        assertThatThrownBy(() -> FileMetadata.create(owner, value, "uploads/a.pdf", "application/pdf", 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_ORIGINAL_FILE_NAME);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("저장 키이 없거나 공백이면 거부한다")
    void rejectBlankObjectKey(String value) {
        assertThatThrownBy(() -> FileMetadata.create(owner, "a.pdf", value, "application/pdf", 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_OBJECT_KEY);
    }

    @Test
    @DisplayName("저장 키이 최대 길이를 초과하면 거부한다")
    void rejectLongObjectKey() {
        String value = "a".repeat(1025);

        assertThatThrownBy(() -> FileMetadata.create(owner, "a.pdf", value, "application/pdf", 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_OBJECT_KEY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("파일 유형이 없거나 공백이면 거부한다")
    void rejectBlankContentType(String value) {
        assertThatThrownBy(() -> FileMetadata.create(owner, "a.pdf", "uploads/a.pdf", value, 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_CONTENT_TYPE);
    }

    @Test
    @DisplayName("파일 유형이 최대 길이를 초과하면 거부한다")
    void rejectLongContentType() {
        String value = "a".repeat(256);

        assertThatThrownBy(() -> FileMetadata.create(owner, "a.pdf", "uploads/a.pdf", value, 1))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.INVALID_CONTENT_TYPE);
    }
}

