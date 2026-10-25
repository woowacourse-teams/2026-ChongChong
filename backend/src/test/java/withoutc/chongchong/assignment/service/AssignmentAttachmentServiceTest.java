package withoutc.chongchong.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.policy.AssignmentAttachmentPolicy;
import withoutc.chongchong.assignment.repository.AssignmentAttachmentRepository;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.user.entity.User;

class AssignmentAttachmentServiceTest {

    private final FileMetaDataRepository fileMetaDataRepository = mock(FileMetaDataRepository.class);
    private final AssignmentAttachmentRepository assignmentAttachmentRepository = mock(AssignmentAttachmentRepository.class);
    private final AssignmentAttachmentService service = new AssignmentAttachmentService(
            fileMetaDataRepository, assignmentAttachmentRepository, mock(AssignmentAttachmentPolicy.class));

    @ParameterizedTest
    @EnumSource(FileUploadStatus.class)
    @DisplayName("타인의 새 파일은 업로드 상태와 관계없이 소유권 오류로 거부한다")
    void rejectsUnownedFileBeforeCheckingStatus(FileUploadStatus status) {
        FileMetadata file = file(2L, status);
        when(fileMetaDataRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> service.prepare(1L, List.of(), List.of(10L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_OWNED);
        verifyNoInteractions(assignmentAttachmentRepository);
    }

    @Test
    @DisplayName("본인 파일이 미완료이면 첨부 불가 오류로 거부한다")
    void rejectsOwnedPendingFile() {
        FileMetadata file = file(1L, FileUploadStatus.PENDING);
        when(fileMetaDataRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> service.prepare(1L, List.of(), List.of(10L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);
    }

    @Test
    @DisplayName("기존 첨부는 다른 소유자의 파일이어도 유지할 수 있다")
    void retainsExistingAttachmentFromAnotherOwner() {
        FileMetadata file = file(2L, FileUploadStatus.COMPLETED);
        when(fileMetaDataRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(file));

        assertThat(service.prepare(1L, List.of(file), List.of(10L))).containsExactly(file);
        verifyNoInteractions(assignmentAttachmentRepository);
    }

    private FileMetadata file(Long ownerId, FileUploadStatus status) {
        User owner = User.create("소유자", null);
        ReflectionTestUtils.setField(owner, "id", ownerId);
        FileMetadata file = FileMetadata.create(owner, "자료.pdf", "test/file", "application/pdf", 1L);
        ReflectionTestUtils.setField(file, "id", 10L);
        ReflectionTestUtils.setField(file, "status", status);
        return file;
    }
}
