package withoutc.chongchong.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.policy.StudyImagePolicy;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class StudyImageService {

    private final FileMetaDataRepository fileRepository;
    private final StudyImagePolicy imagePolicy;

    public FileMetadata prepareForCreate(Long userId, Long fileId) {
        if (fileId == null) {
            return null;
        }
        imagePolicy.validateId(fileId);

        FileMetadata file = fileRepository.getByIdForUpdateOrThrow(fileId);
        validateNewFile(userId, file);
        return file;
    }

    public FileMetadata prepareForUpdate(Long userId, Study study, Long fileId) {
        if (fileId == null) {
            return study.getImageFile();
        }
        imagePolicy.validateId(fileId);

        FileMetadata file = fileRepository.getByIdForUpdateOrThrow(fileId);
        if (study.getImageFile() != null && study.getImageFile().getId().equals(fileId)) {
            validateCompletedFile(file);
            return file;
        }
        validateNewFile(userId, file);
        return file;
    }

    public void prepareForDelete(Study study) {
        if (study.getImageFile() == null) {
            return;
        }
        Long fileId = study.getImageFile().getId();
        FileMetadata file = fileRepository.getByIdForUpdateOrThrow(fileId);
        validateCompletedFile(file);
    }

    private void validateNewFile(Long userId, FileMetadata file) {
        if (!file.isOwnedBy(userId)) {
            throw new FileException(FileErrorCode.FILE_NOT_OWNED);
        }
        validateCompletedFile(file);
        imagePolicy.validateImage(file);
        if (fileRepository.existsAnyUsageByFileId(file.getId())) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
    }

    private void validateCompletedFile(FileMetadata file) {
        if (file.getStatus() != FileUploadStatus.COMPLETED) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
    }
}
