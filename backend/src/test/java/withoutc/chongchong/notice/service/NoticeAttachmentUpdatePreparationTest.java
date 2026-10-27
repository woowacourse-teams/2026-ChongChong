package withoutc.chongchong.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.notice.repository.NoticeAttachmentRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.policy.NoticeAttachmentPolicy;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class NoticeAttachmentUpdatePreparationTest {

    private static final Long USER_ID = 1L;
    private static final Long PREVIOUS_LEADER_ID = 2L;

    @Mock
    private FileMetaDataRepository fileRepository;

    @Mock
    private NoticeAttachmentRepository attachmentRepository;

    private NoticeAttachmentService attachmentService;
    private NoticeAttachmentPolicy attachmentPolicy;
    private Notice notice;

    @BeforeEach
    void setUp() {
        attachmentPolicy = new NoticeAttachmentPolicy(10, 5242880, List.of("image/jpeg", "image/png", "image/webp"));
        attachmentService = new NoticeAttachmentService(fileRepository, attachmentRepository, attachmentPolicy);
        notice = Notice.create(mock(Study.class), "기존 공지", "기존 내용");
    }

    @Test
    @DisplayName("수정 첨부 목록이 null이면 현재 제한을 초과한 기존 첨부도 순서대로 유지한다")
    void preserveExistingAttachmentsWhenOmitted() {
        FileMetadata first = completedFile(7L);
        FileMetadata second = completedFile(31L);
        notice.addAttachments(List.of(first, second));
        attachmentService = new NoticeAttachmentService(fileRepository, attachmentRepository,
                new NoticeAttachmentPolicy(1, 1, List.of("image/jpeg")));

        assertThat(attachmentService.prepareForUpdate(USER_ID, notice, null)).containsExactly(first, second);

        verifyNoInteractions(fileRepository, attachmentRepository);
    }

    @Test
    @DisplayName("빈 목록은 기존 파일을 잠금 조회하고 빈 최종 목록을 반환하며 아직 첨부를 제거하지 않는다")
    void prepareRemovalOfAllAttachments() {
        FileMetadata file = completedFile(7L);
        notice.addAttachments(List.of(file));
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThat(attachmentService.prepareForUpdate(USER_ID, notice, List.of())).isEmpty();

        verify(fileRepository).findAllByIdForUpdate(List.of(7L));
        verifyNoInteractions(attachmentRepository);
        assertThat(notice.getAttachments()).hasSize(1);
        assertThat(file.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("기존 첨부도 최종 목록도 없으면 DB 조회와 연결 검사을 생략한다")
    void prepareEmptyAttachments() {
        assertThat(attachmentService.prepareForUpdate(USER_ID, notice, List.of())).isEmpty();

        verifyNoInteractions(fileRepository, attachmentRepository);
    }

    @Test
    @DisplayName("이전 리더의 기존 첨부는 변경된 형식과 용량 정책 및 S3 검증 없이 재정렬할 수 있다")
    void reorderExistingFilesWithoutRevalidation() {
        FileMetadata first = completedFile(7L);
        FileMetadata second = completedFile(31L);
        notice.addAttachments(List.of(first, second));
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 31L))).thenReturn(List.of(first, second));
        attachmentService = new NoticeAttachmentService(fileRepository, attachmentRepository,
                new NoticeAttachmentPolicy(2, 1, List.of("image/jpeg")));

        assertThat(attachmentService.prepareForUpdate(USER_ID, notice, List.of(31L, 7L)))
                .containsExactly(second, first);
        assertThat(notice.getAttachments()).extracting(attachment -> attachment.getFile().getId())
                .containsExactly(7L, 31L);
    }

    @Test
    @DisplayName("유지·제거·추가할 파일을 한 번에 ID 순서로 잠그고 새 파일만 검증해 최종 순서로 반환한다")
    void prepareMixedAttachmentsInRequestedOrder() {
        FileMetadata retained = completedFile(7L);
        FileMetadata removed = completedFile(19L);
        FileMetadata added = createFile(31L, USER_ID);
        notice.addAttachments(List.of(retained, removed));
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 19L, 31L)))
                .thenReturn(List.of(retained, removed, added));

        assertThat(attachmentService.prepareForUpdate(USER_ID, notice, List.of(31L, 7L)))
                .containsExactly(added, retained);

        verify(fileRepository).findAllByIdForUpdate(List.of(7L, 19L, 31L));
        verifyNoMoreInteractions(fileRepository);
        verify(attachmentRepository).existsAnyByFileId(added.getId());
        verifyNoMoreInteractions(attachmentRepository);
        assertThat(notice.getAttachments()).extracting(attachment -> attachment.getFile().getId())
                .containsExactly(7L, 19L);
        assertThat(retained.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(removed.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(added.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    @ParameterizedTest
    @MethodSource("invalidIds")
    @DisplayName("수정 첨부 ID의 null 원소·0·음수·중복은 DB 조회 전에 거부한다")
    void rejectInvalidIds(List<Long> fileIds) {
        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, fileIds))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_ATTACHMENT_IDS);

        verifyNoInteractions(fileRepository, attachmentRepository);
    }

    static Stream<List<Long>> invalidIds() {
        return Stream.of(Collections.singletonList(null), List.of(0L), List.of(-1L), List.of(7L, 7L));
    }

    @Test
    @DisplayName("기존 첨부를 포함한 최종 목록이 설정한 최대 개수를 넘으면 DB 조회 전에 거부한다")
    void rejectTooManyFinalAttachments() {
        notice.addAttachments(List.of(completedFile(1L)));
        List<Long> fileIds = LongStream.rangeClosed(1, 11).boxed().toList();

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, fileIds))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.ATTACHMENT_LIMIT_EXCEEDED);

        verifyNoInteractions(fileRepository, attachmentRepository);
    }

    @Test
    @DisplayName("새로 추가할 파일이 존재하지 않으면 연결 여부 조회 전에 거부한다")
    void rejectMissingNewFile() {
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of());

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_FOUND);

        verifyNoInteractions(attachmentRepository);
    }

    @Test
    @DisplayName("다른 사람의 미연결 파일은 새 첨부로 추가할 수 없다")
    void rejectOtherUsersNewFile() {
        FileMetadata file = createFile(7L, PREVIOUS_LEADER_ID);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_OWNED);

        verifyNoInteractions(attachmentRepository);
    }

    @ParameterizedTest
    @EnumSource(value = FileUploadStatus.class, names = {"PENDING", "DELETE_PENDING"})
    @DisplayName("요청자 소유여도 현재 공지의 첨부가 아닌 미완료·삭제 대기 파일은 추가할 수 없다")
    void rejectNonCompletedNewFile(FileUploadStatus status) {
        FileMetadata file = createFile(7L, USER_ID);
        ReflectionTestUtils.setField(file, "status", status);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);

        verifyNoInteractions(attachmentRepository);
    }

    @ParameterizedTest
    @EnumSource(value = FileUploadStatus.class, names = {"PENDING", "DELETE_PENDING"})
    @DisplayName("현재 공지에 연결되어 있어도 완료 상태가 아닌 파일은 유지할 수 없다")
    void rejectNonCompletedExistingFile(FileUploadStatus status) {
        FileMetadata file = createFile(7L, PREVIOUS_LEADER_ID);
        ReflectionTestUtils.setField(file, "status", status);
        notice.addAttachments(List.of(file));
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);

        verifyNoInteractions(attachmentRepository);
    }

    @Test
    @DisplayName("새 첨부의 형식이 허용되지 않으면 연결 여부 조회 전에 거부한다")
    void rejectUnsupportedNewFileType() {
        FileMetadata file = createFile(7L, USER_ID, "application/pdf", 1024);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L)))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_TYPE);

        verifyNoInteractions(attachmentRepository);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5242881L})
    @DisplayName("새 첨부가 빈 파일이거나 용량 제한을 넘으면 연결 여부 조회 전에 거부한다")
    void rejectInvalidNewFileSize(long size) {
        FileMetadata file = createFile(7L, USER_ID, "image/png", size);
        when(fileRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L)))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_SIZE);

        verifyNoInteractions(attachmentRepository);
    }

    @Test
    @DisplayName("새 파일의 소유권 검증이 실패해도 모든 파일은 완료 상태를 유지한다")
    void preserveCompletedFilesWhenOwnershipFails() {
        FileMetadata valid = createFile(7L, USER_ID);
        FileMetadata invalid = createFile(31L, PREVIOUS_LEADER_ID);
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 31L))).thenReturn(List.of(valid, invalid));

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L, 31L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_OWNED);

        assertThat(valid.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(invalid.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("일부 새 파일이 이미 연결되어 있으면 기존 첨부와 완료 상태를 유지한다")
    void preserveAttachmentsWhenNewFileAlreadyAttached() {
        FileMetadata existing = completedFile(3L);
        FileMetadata first = createFile(7L, USER_ID);
        FileMetadata second = createFile(31L, USER_ID);
        notice.addAttachments(List.of(existing));
        when(fileRepository.findAllByIdForUpdate(List.of(3L, 7L, 31L)))
                .thenReturn(List.of(existing, first, second));
        when(attachmentRepository.existsAnyByFileId(7L)).thenReturn(false);
        when(attachmentRepository.existsAnyByFileId(31L)).thenReturn(true);

        assertThatThrownBy(() -> attachmentService.prepareForUpdate(USER_ID, notice, List.of(7L, 31L)))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);

        verify(attachmentRepository).existsAnyByFileId(7L);
        assertThat(notice.getAttachments()).extracting(attachment -> attachment.getFile().getId())
                .containsExactly(3L);
        assertThat(List.of(existing, first, second)).allMatch(FileMetadata::isCompleted);
    }

    @Test
    @DisplayName("미연결 완료 파일은 기존 첨부와 함께 최종 순서로 추가할 수 있다")
    void acceptCompletedNewAttachmentWithExistingFile() {
        FileMetadata existing = completedFile(7L);
        FileMetadata added = createFile(31L, USER_ID);
        notice.addAttachments(List.of(existing));
        when(fileRepository.findAllByIdForUpdate(List.of(7L, 31L))).thenReturn(List.of(existing, added));

        assertThat(attachmentService.prepareForUpdate(USER_ID, notice, List.of(31L, 7L)))
                .containsExactly(added, existing);

        verify(attachmentRepository).existsAnyByFileId(31L);
        verifyNoMoreInteractions(attachmentRepository);
        assertThat(existing.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(added.getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(notice.getAttachments()).hasSize(1);
    }

    private FileMetadata completedFile(Long id) {
        return createFile(id, PREVIOUS_LEADER_ID);
    }

    private FileMetadata createFile(Long id, Long ownerId) {
        return createFile(id, ownerId, "image/png", 1024);
    }

    private FileMetadata createFile(Long id, Long ownerId, String contentType, long size) {
        User owner = User.create("파일 소유자", null);
        ReflectionTestUtils.setField(owner, "id", ownerId);
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/" + id, contentType, size);
        file.completeUpload();
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }
}
