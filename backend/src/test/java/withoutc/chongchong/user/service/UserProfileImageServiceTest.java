package withoutc.chongchong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.policy.UserProfileImagePolicy;

class UserProfileImageServiceTest {

    private final FileMetaDataRepository fileRepository = mock(FileMetaDataRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final UserProfileImagePolicy imagePolicy = new UserProfileImagePolicy(
            5242880, List.of("image/jpeg", "image/png", "image/webp"), 1024);
    private final UserProfileImageService service = new UserProfileImageService(
            fileRepository, imagePolicy, entityManager);
    private final User user = user(1L);

    @Test
    @DisplayName("본인 소유의 완료된 미사용 이미지를 준비하되 직접 연결하거나 상태를 바꾸지 않는다")
    void preparesNewImageWithoutChangingUserOrFile() {
        FileMetadata file = completedFile(10L, 1L);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThat(service.prepareForUpdate(user, 10L)).isSameAs(file);
        assertThat(user.getProfileImageFile()).isNull();
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        verify(fileRepository).existsAnyUsageByFileId(10L);
        verify(entityManager).refresh(file);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("잘못된 파일 식별자는 파일을 조회하기 전에 거절한다")
    void rejectsInvalidIdBeforeLocking(Long fileId) {
        assertThatThrownBy(() -> service.prepareForUpdate(user, fileId))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_USER_PROFILE_IMAGE_ID));
        verifyNoInteractions(fileRepository, entityManager);
    }

    @Test
    @DisplayName("존재하지 않는 파일은 찾을 수 없음으로 거절한다")
    void rejectsMissingFile() {
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenThrow(new FileException(FileErrorCode.FILE_NOT_FOUND));

        assertThatThrownBy(() -> service.prepareForUpdate(user, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("새 파일과 현재 파일 재지정 모두 본인 소유여야 한다")
    void rejectsFileOwnedByAnotherUser(boolean current) {
        FileMetadata file = completedFile(10L, 2L);
        if (current) {
            user.changeProfileImage(file);
        }
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThatThrownBy(() -> service.prepareForUpdate(user, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_OWNED));
        verify(fileRepository, never()).existsAnyUsageByFileId(10L);
    }

    @ParameterizedTest
    @EnumSource(value = FileUploadStatus.class, names = {"PENDING", "DELETE_PENDING"})
    @DisplayName("미완료·삭제 대기 상태는 신규 연결과 현재 파일 재지정 모두 거절한다")
    void rejectsIncompleteFile(FileUploadStatus status) {
        FileMetadata file = completedFile(10L, 1L);
        ReflectionTestUtils.setField(file, "status", status);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThatThrownBy(() -> service.prepareForUpdate(user, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE));
        user.changeProfileImage(file);
        assertThatThrownBy(() -> service.prepareForUpdate(user, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE));
    }

    @Test
    @DisplayName("이미 다른 대상에서 사용 중인 파일은 새 홈 이미지로 연결할 수 없다")
    void rejectsAlreadyUsedFile() {
        FileMetadata file = completedFile(10L, 1L);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);
        when(fileRepository.existsAnyUsageByFileId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.prepareForUpdate(user, 10L))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE));
        assertThat(user.getProfileImageFile()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"image/svg+xml, 1024", "image/png, 5242881"})
    @DisplayName("현재 파일 재지정은 변경된 형식·용량 정책과 기존 사용처 검사를 다시 적용하지 않는다")
    void retainsCurrentImageDespiteChangedPolicy(String contentType, long size) {
        FileMetadata file = file(10L, 1L, contentType, size);
        user.changeProfileImage(file);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThat(service.prepareForUpdate(user, 10L)).isSameAs(file);
        verify(fileRepository, never()).existsAnyUsageByFileId(10L);
    }

    @ParameterizedTest
    @CsvSource({"image/gif, 1024, INVALID_USER_PROFILE_IMAGE_TYPE", "image/png, 1025, INVALID_USER_PROFILE_IMAGE_SIZE"})
    @DisplayName("새 이미지는 형식과 공통 업로드 제한을 포함한 이미지 정책을 적용한다")
    void appliesImagePolicyToNewFile(String contentType, long size, UserErrorCode expectedError) {
        FileMetadata file = file(10L, 1L, contentType, size);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        assertThatThrownBy(() -> service.prepareForUpdate(user, 10L))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedError));
    }

    @ParameterizedTest
    @CsvSource({"10, 20", "20, 10"})
    @DisplayName("교체할 기존 파일과 새 파일은 ID 오름차순으로 잠그고 다시 읽는다")
    void locksBothFilesInAscendingOrder(long oldId, long newId) {
        FileMetadata oldFile = completedFile(oldId, 1L);
        FileMetadata newFile = completedFile(newId, 1L);
        user.changeProfileImage(oldFile);
        when(fileRepository.getByIdForUpdateOrThrow(oldId)).thenReturn(oldFile);
        when(fileRepository.getByIdForUpdateOrThrow(newId)).thenReturn(newFile);

        assertThat(service.prepareForUpdate(user, newId)).isSameAs(newFile);

        FileMetadata first = oldId < newId ? oldFile : newFile;
        FileMetadata second = oldId < newId ? newFile : oldFile;
        InOrder order = inOrder(fileRepository, entityManager);
        order.verify(fileRepository).getByIdForUpdateOrThrow(first.getId());
        order.verify(entityManager).refresh(first);
        order.verify(fileRepository).getByIdForUpdateOrThrow(second.getId());
        order.verify(entityManager).refresh(second);
        order.verify(fileRepository).existsAnyUsageByFileId(newId);
        assertThat(user.getProfileImageFile()).isSameAs(oldFile);
    }

    @Test
    @DisplayName("파일 연결이 없는 홈 이미지 초기화는 파일을 조회하지 않는다")
    void preparesResetWithoutFile() {
        service.prepareForReset(user);

        verifyNoInteractions(fileRepository, entityManager);
    }

    @Test
    @DisplayName("초기화 준비는 현재 파일을 잠그며 직접 연결 해제나 상태 변경을 수행하지 않는다")
    void locksCurrentFileForReset() {
        FileMetadata file = completedFile(10L, 1L);
        user.changeProfileImage(file);
        when(fileRepository.getByIdForUpdateOrThrow(10L)).thenReturn(file);

        service.prepareForReset(user);

        verify(entityManager).refresh(file);
        verify(fileRepository, never()).existsAnyUsageByFileId(10L);
        assertThat(user.getProfileImageFile()).isSameAs(file);
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    private FileMetadata completedFile(Long id, Long ownerId) {
        return file(id, ownerId, "image/png", 1024);
    }

    private FileMetadata file(Long id, Long ownerId, String contentType, long size) {
        FileMetadata file = FileMetadata.create(user(ownerId), "image.png", "uploads/" + id, contentType, size);
        file.completeUpload();
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }

    private User user(Long id) {
        User owner = User.create("소유자", "https://example.com/social.png");
        ReflectionTestUtils.setField(owner, "id", id);
        return owner;
    }
}
