package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.policy.StudyImagePolicy;
import withoutc.chongchong.user.entity.User;

class StudyImageServiceTest {

    private final FileMetaDataRepository fileRepository = mock(FileMetaDataRepository.class);
    private final StudyImagePolicy imagePolicy = mock(StudyImagePolicy.class);
    private final StudyImageService service = new StudyImageService(fileRepository, imagePolicy);

    @Test
    @DisplayName("이미 연결된 파일은 새 스터디 이미지로 거부한다")
    void rejectsAlreadyLinkedFile() {
        FileMetadata file = file(1L);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);
        when(fileRepository.existsAnyUsageByFileId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.prepareForCreate(1L, 10L))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);
    }

    @Test
    @DisplayName("타인이 업로드한 파일은 스터디 생성과 이미지 교체에 사용할 수 없다")
    void rejectsFileOwnedByAnotherUser() {
        FileMetadata file = file(2L);
        Study study = Study.create("스터디", "설명", false);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThatThrownBy(() -> service.prepareForCreate(1L, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_OWNED));
        assertThatThrownBy(() -> service.prepareForUpdate(1L, study, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_OWNED));
    }

    @ParameterizedTest
    @EnumSource(value = FileUploadStatus.class, names = {"PENDING", "DELETE_PENDING"})
    @DisplayName("업로드 미완료 또는 삭제 대기 파일은 생성과 교체에 사용할 수 없다")
    void rejectsFileThatIsNotCompleted(FileUploadStatus status) {
        FileMetadata file = file(1L);
        ReflectionTestUtils.setField(file, "status", status);
        Study study = Study.create("스터디", "설명", false);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThatThrownBy(() -> service.prepareForCreate(1L, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE));
        assertThatThrownBy(() -> service.prepareForUpdate(1L, study, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE));
    }

    @Test
    @DisplayName("같은 이미지를 유지할 때 새 파일의 소유권과 중복 연결을 검사하지 않는다")
    void retainsCurrentImage() {
        FileMetadata file = file(2L);
        Study study = Study.create("스터디", "설명", false);
        ReflectionTestUtils.setField(study, "imageFile", file);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThat(service.prepareForUpdate(1L, study, 10L)).isSameAs(file);
        verify(fileRepository, never()).existsAnyUsageByFileId(10L);
        verify(imagePolicy, never()).validateImage(file);
    }

    @Test
    @DisplayName("이미지가 없는 스터디를 삭제할 때 파일을 조회하지 않는다")
    void preparesStudyWithoutImage() {
        service.prepareForDelete(Study.create("스터디", "설명", false));

        verifyNoInteractions(fileRepository);
    }

    @Test
    @DisplayName("이미지 파일 ID를 생략하면 생성 시 이미지가 없다")
    void createsWithoutImage() {
        assertThat(service.prepareForCreate(1L, null)).isNull();

        verifyNoInteractions(fileRepository, imagePolicy);
    }

    private FileMetadata file(Long ownerId) {
        User owner = User.create("소유자", null);
        ReflectionTestUtils.setField(owner, "id", ownerId);
        FileMetadata file = FileMetadata.create(owner, "image.png", "study/image", "image/png", 1024);
        ReflectionTestUtils.setField(file, "id", 10L);
        ReflectionTestUtils.setField(file, "status", FileUploadStatus.COMPLETED);
        return file;
    }
}
