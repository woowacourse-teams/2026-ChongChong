package withoutc.chongchong.assignment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.policy.AssignmentAttachmentPolicy;
import withoutc.chongchong.assignment.repository.AssignmentAttachmentRepository;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;

@Service
@Transactional(propagation = Propagation.MANDATORY)
@RequiredArgsConstructor
public class AssignmentAttachmentService {

    private final FileMetaDataRepository fileMetaDataRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;

    private final AssignmentAttachmentPolicy assignmentAttachmentPolicy;

    public List<FileMetadata> prepare(Long userId, List<FileMetadata> existing, List<Long> requestedIds) {
        if (requestedIds == null) {
            return existing;
        }
        assignmentAttachmentPolicy.validateIds(requestedIds);

        Set<Long> existingIds = existing.stream().map(FileMetadata::getId).collect(Collectors.toSet());
        List<Long> ids = requestedIds.stream().sorted().toList();
        Map<Long, FileMetadata> locked = lockFiles(ids);

        List<FileMetadata> files = new ArrayList<>();
        for (Long fileId : requestedIds) {
            FileMetadata file = getFile(locked, fileId);
            boolean isExisting = existingIds.contains(file.getId());
            if (!isExisting && !file.isOwnedBy(userId)) {
                throw new FileException(FileErrorCode.FILE_NOT_OWNED);
            }
            if (!file.isCompleted()) {
                throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
            }
            if (!isExisting) {
                validateNewFile(file);
            }
            files.add(file);
        }

        return files;
    }

    private FileMetadata getFile(Map<Long, FileMetadata> filesById, Long fileId) {
        FileMetadata file = filesById.get(fileId);
        if (file == null) {
            throw new FileException(FileErrorCode.FILE_NOT_FOUND);
        }
        return file;
    }

    private void validateNewFile(FileMetadata file) {
        Long fileId = file.getId();
        if (assignmentAttachmentRepository.existsByFileId(fileId)
                || assignmentAttachmentRepository.existsSubmissionByFileId(fileId)
                || assignmentAttachmentRepository.existsNoticeByFileId(fileId)) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
        assignmentAttachmentPolicy.validateFile(file);
    }

    private Map<Long, FileMetadata> lockFiles(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return fileMetaDataRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(FileMetadata::getId, file -> file));
    }
}
