package withoutc.chongchong.file.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.user.entity.User;

class FileMetadataTest {

    private final User owner = User.create("사용자", null);

    @Test
    @DisplayName("업로드 대기 파일을 완료 상태로 전환한다")
    void completePendingUpload() {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/image", "image/png", 1024);

        file.completeUpload();

        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("이미 완료된 파일은 다시 완료 처리할 수 없다")
    void rejectCompletingUploadTwice() {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/image", "image/png", 1024);
        file.completeUpload();

        assertThatThrownBy(file::completeUpload)
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("완료 파일을 삭제 대기로 전환하고 실제 삭제에 필요한 메타데이터는 보존한다")
    void markCompletedFileForDeletion() {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/image", "image/png", 1024);
        file.completeUpload();

        file.markForDeletion();

        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.DELETE_PENDING);
        assertThat(file.getObjectKey()).isEqualTo("uploads/image");
        assertThat(file.getOwner()).isSameAs(owner);
        assertThat(file.getOriginalFileName()).isEqualTo("image.png");
        assertThat(file.getContentType()).isEqualTo("image/png");
        assertThat(file.getSize()).isEqualTo(1024);
    }

    @Test
    @DisplayName("업로드 대기 파일은 첨부 제거를 위한 삭제 대기로 전환할 수 없다")
    void rejectDeletingPendingFile() {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/image", "image/png", 1024);

        assertThatThrownBy(file::markForDeletion)
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.PENDING);
    }

    @Test
    @DisplayName("삭제 대기 파일은 다시 완료 처리하여 재사용할 수 없다")
    void rejectCompletingFilePendingDeletion() {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/image", "image/png", 1024);
        file.completeUpload();
        file.markForDeletion();

        assertThatThrownBy(file::completeUpload)
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.DELETE_PENDING);
    }

    @Test
    @DisplayName("삭제 대기 파일은 다시 삭제 대기로 전환할 수 없다")
    void rejectMarkingFileForDeletionTwice() {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/image", "image/png", 1024);
        file.completeUpload();
        file.markForDeletion();

        assertThatThrownBy(file::markForDeletion)
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.DELETE_PENDING);
    }

    @Test
    @DisplayName("파일 소유자의 사용자 ID로 본인 소유 여부를 확인한다")
    void checkOwnershipByUserId() {
        ReflectionTestUtils.setField(owner, "id", 1L);
        FileMetadata file = FileMetadata.create(owner, "a.png", "uploads/a", "image/png", 1);

        assertThat(file.isOwnedBy(1L)).isTrue();
        assertThat(file.isOwnedBy(2L)).isFalse();
        assertThat(file.isOwnedBy(null)).isFalse();
    }

    @Test
    @DisplayName("소유자의 ID가 없으면 같은 null ID라도 본인 소유로 판단하지 않는다")
    void rejectOwnershipWithoutPersistedOwnerId() {
        FileMetadata file = FileMetadata.create(owner, "a.png", "uploads/a", "image/png", 1);

        assertThat(file.isOwnedBy(null)).isFalse();
        assertThat(file.isOwnedBy(1L)).isFalse();
    }

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
