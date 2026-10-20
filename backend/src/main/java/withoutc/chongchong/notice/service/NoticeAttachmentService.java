package withoutc.chongchong.notice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.UploadedFileVerifier;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeAttachment;
import withoutc.chongchong.notice.policy.NoticeAttachmentPolicy;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeAttachmentService {

    private final FileMetaDataRepository fileRepository;
    private final UploadedFileVerifier uploadedFileVerifier;
    private final NoticeAttachmentPolicy attachmentPolicy;

    public List<FileMetadata> prepareForCreate(Long userId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        attachmentPolicy.validateIds(fileIds);

        List<FileMetadata> files = getFilesInOrder(findFilesForUpdate(fileIds), fileIds);
        validateNewFiles(userId, files);
        return files;
    }

    // 호출자는 공지 쓰기 잠금을 획득한 트랜잭션에서 수정 준비와 저장을 함께 수행한다.
    public List<FileMetadata> prepareForUpdate(Long userId, Notice notice, List<Long> fileIds) {
        if (fileIds == null) {
            return notice.getAttachments().stream().map(NoticeAttachment::getFile).toList();
        }
        attachmentPolicy.validateIds(fileIds);

        Set<Long> existingIds = notice.getAttachments().stream().map(attachment -> attachment.getFile().getId())
                .collect(Collectors.toSet());
        List<Long> idsToLock = Stream.concat(existingIds.stream(), fileIds.stream()).distinct().toList();
        List<FileMetadata> files = getFilesInOrder(findFilesForUpdate(idsToLock), fileIds);
        files.stream().filter(file -> existingIds.contains(file.getId())).forEach(this::validateExistingFile);
        List<FileMetadata> newFiles = files.stream().filter(file -> !existingIds.contains(file.getId())).toList();
        validateNewFiles(userId, newFiles);
        return files;
    }

    private void validateNewFiles(Long userId, List<FileMetadata> files) {
        for (FileMetadata file : files) {
            validateAttachableFile(userId, file);
        }
        files.forEach(uploadedFileVerifier::verify);
    }

    private Map<Long, FileMetadata> findFilesForUpdate(List<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        List<Long> sortedIds = fileIds.stream().sorted().toList();
        return fileRepository.findAllByIdForUpdate(sortedIds).stream()
                .collect(Collectors.toMap(FileMetadata::getId, file -> file));
    }

    private List<FileMetadata> getFilesInOrder(Map<Long, FileMetadata> filesById, List<Long> fileIds) {
        return fileIds.stream().map(id -> getFile(filesById, id)).toList();
    }

    private FileMetadata getFile(Map<Long, FileMetadata> filesById, Long id) {
        FileMetadata file = filesById.get(id);
        if (file == null) {
            throw new FileException(FileErrorCode.FILE_NOT_FOUND);
        }
        return file;
    }

    private void validateAttachableFile(Long userId, FileMetadata file) {
        if (!file.isOwnedBy(userId)) {
            throw new FileException(FileErrorCode.FILE_NOT_OWNED);
        }
        if (file.getStatus() != FileUploadStatus.PENDING) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
        attachmentPolicy.validateImage(file);
    }

    private void validateExistingFile(FileMetadata file) {
        if (file.getStatus() != FileUploadStatus.COMPLETED) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
    }
}
