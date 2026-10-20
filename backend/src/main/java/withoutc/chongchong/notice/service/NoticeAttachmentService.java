package withoutc.chongchong.notice.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.UploadedFileVerifier;
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

        List<FileMetadata> files = findFilesForUpdate(fileIds);
        for (FileMetadata file : files) {
            validateAttachableFile(userId, file);
        }
        files.forEach(uploadedFileVerifier::verify);
        return files;
    }

    private List<FileMetadata> findFilesForUpdate(List<Long> fileIds) {
        List<Long> sortedIds = fileIds.stream().sorted().toList();
        Map<Long, FileMetadata> filesById = fileRepository.findAllByIdForUpdate(sortedIds).stream()
                .collect(Collectors.toMap(FileMetadata::getId, file -> file));
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
}
