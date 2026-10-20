package withoutc.chongchong.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.UnavailableUploadedFileVerifier;
import withoutc.chongchong.file.service.UploadedFileVerifier;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.policy.NoticeAttachmentPolicy;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class NoticeAttachmentServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FileMetaDataRepository fileRepository;

    @Mock
    private UploadedFileVerifier uploadedFileVerifier;

    private NoticeAttachmentService attachmentService;
    private NoticeAttachmentPolicy attachmentPolicy;

    @BeforeEach
    void setUp() {
        attachmentPolicy = new NoticeAttachmentPolicy(10, 5242880, List.of("image/jpeg", "image/png", "image/webp"));
        attachmentService = new NoticeAttachmentService(fileRepository, uploadedFileVerifier, attachmentPolicy);
    }

    @Test
    @DisplayName("첨부 목록이 null이거나 비어 있으면 DB 조회와 업로드 검증을 생략한다")
    void prepareWithoutAttachments() {
        assertThat(attachmentService.prepareForCreate(USER_ID, null)).isEmpty();
        assertThat(attachmentService.prepareForCreate(USER_ID, List.of())).isEmpty();

        verifyNoInteractions(fileRepository, uploadedFileVerifier);
    }

    @Test
    @DisplayName("ID 순서로 잠금 조회하고 요청한 표시 순서로 파일을 검증하여 반환한다")
    void prepareInRequestedOrder() {
        FileMetadata first = createFile(7L, USER_ID);
        FileMetadata second = createFile(31L, USER_ID);
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 31L))).thenReturn(List.of(first, second));

        List<FileMetadata> files = attachmentService.prepareForCreate(USER_ID, List.of(31L, 7L));

        assertThat(files).containsExactly(second, first);
        InOrder verificationOrder = inOrder(uploadedFileVerifier);
        verificationOrder.verify(uploadedFileVerifier).verify(second);
        verificationOrder.verify(uploadedFileVerifier).verify(first);
        assertThat(files).allMatch(file -> file.getStatus() == FileUploadStatus.PENDING);
    }

    @Test
    @DisplayName("일부 파일이 존재하지 않으면 실제 업로드 검증 전에 거부한다")
    void rejectMissingFile() {
        FileMetadata existing = createFile(7L, USER_ID);
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 31L))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L, 31L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_FOUND);

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("다른 사용자가 업로드한 파일은 첨부할 수 없다")
    void rejectOtherUsersFile() {
        FileMetadata file = createFile(7L, 2L);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_OWNED);

        verifyNoInteractions(uploadedFileVerifier);
    }

    @ParameterizedTest
    @EnumSource(value = FileUploadStatus.class, names = {"COMPLETED", "DELETE_PENDING"})
    @DisplayName("완료되었거나 삭제 대기 중인 파일은 새 공지에 첨부할 수 없다")
    void rejectNonPendingFile(FileUploadStatus status) {
        FileMetadata file = createFile(7L, USER_ID);
        ReflectionTestUtils.setField(file, "status", status);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("모든 파일의 소유권을 확인하기 전에는 실제 업로드 검증을 호출하지 않는다")
    void rejectBeforeVerifyingAnyFile() {
        FileMetadata owned = createFile(7L, USER_ID);
        FileMetadata otherUsers = createFile(31L, 2L);
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 31L))).thenReturn(List.of(owned, otherUsers));

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L, 31L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_OWNED);

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("중복 첨부 파일 ID는 DB 조회 전에 거부한다")
    void rejectDuplicateFileIds() {
        assertInvalidIds(List.of(7L, 7L));
    }

    @Test
    @DisplayName("null 첨부 파일 ID는 DB 조회 전에 거부한다")
    void rejectNullFileId() {
        assertInvalidIds(Collections.singletonList(null));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    @DisplayName("양수가 아닌 첨부 파일 ID는 DB 조회 전에 거부한다")
    void rejectNonPositiveFileId(long fileId) {
        assertInvalidIds(List.of(fileId));
    }

    @Test
    @DisplayName("첨부 개수 제한을 초과하면 DB 조회 전에 거부한다")
    void rejectTooManyAttachments() {
        List<Long> fileIds = LongStream.rangeClosed(1, 11).boxed().toList();

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, fileIds))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.ATTACHMENT_LIMIT_EXCEEDED);

        verifyNoInteractions(fileRepository, uploadedFileVerifier);
    }

    @Test
    @DisplayName("허용하지 않는 형식의 파일은 실제 업로드 검증 전에 거부한다")
    void rejectUnsupportedImageType() {
        FileMetadata file = createFile(7L, USER_ID, "application/pdf", 1024);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L)))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_TYPE);

        verifyNoInteractions(uploadedFileVerifier);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5242881L})
    @DisplayName("빈 이미지나 용량을 초과한 이미지는 실제 업로드 검증 전에 거부한다")
    void rejectInvalidImageSize(long size) {
        FileMetadata file = createFile(7L, USER_ID, "image/png", size);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L)))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_SIZE);

        verifyNoInteractions(uploadedFileVerifier);
    }

    @Test
    @DisplayName("실제 업로드 검증에 실패해도 파일 상태를 변경하지 않는다")
    void preserveStatusWhenVerificationFails() {
        FileMetadata first = createFile(7L, USER_ID);
        FileMetadata second = createFile(31L, USER_ID);
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 31L))).thenReturn(List.of(first, second));
        doNothing().when(uploadedFileVerifier).verify(first);
        doThrow(new FileException(FileErrorCode.FILE_VERIFICATION_UNAVAILABLE))
                .when(uploadedFileVerifier).verify(second);

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L, 31L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_VERIFICATION_UNAVAILABLE);

        assertThat(first.getStatus()).isEqualTo(FileUploadStatus.PENDING);
        assertThat(second.getStatus()).isEqualTo(FileUploadStatus.PENDING);
    }

    @Test
    @DisplayName("기본 업로드 검증기는 본인 소유의 대기 파일도 검증 불가로 거부한다")
    void rejectWithDefaultVerifier() {
        attachmentService = new NoticeAttachmentService(
                fileRepository, new UnavailableUploadedFileVerifier(), attachmentPolicy
        );
        FileMetadata file = createFile(7L, USER_ID);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, List.of(7L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_VERIFICATION_UNAVAILABLE);

        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.PENDING);
    }

    private void assertInvalidIds(List<Long> fileIds) {
        assertThatThrownBy(() -> attachmentService.prepareForCreate(USER_ID, fileIds))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_ATTACHMENT_IDS);

        verifyNoInteractions(fileRepository, uploadedFileVerifier);
    }

    private FileMetadata createFile(Long fileId, Long ownerId) {
        return createFile(fileId, ownerId, "image/png", 1024);
    }

    private FileMetadata createFile(Long fileId, Long ownerId, String contentType, long size) {
        User owner = User.create("사용자", null);
        ReflectionTestUtils.setField(owner, "id", ownerId);
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/" + fileId, contentType, size);
        ReflectionTestUtils.setField(file, "id", fileId);
        return file;
    }
}
