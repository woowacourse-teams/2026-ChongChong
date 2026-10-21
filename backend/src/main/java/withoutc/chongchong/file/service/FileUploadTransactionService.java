package withoutc.chongchong.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.policy.FilePolicy;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.repository.projection.FileUploadSnapshotProjection;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileUploadTransactionService {

    private final FileMetaDataRepository fileMetaDataRepository;
    private final FilePolicy filePolicy;

    public FileUploadSnapshotProjection getOwnedFile(Long userId, Long fileId) {
        FileUploadSnapshotProjection file = fileMetaDataRepository.getUploadSnapshotByIdOrThrow(fileId);
        filePolicy.validateCompletion(userId, file.ownerId(), file.status());

        return file;
    }

    @Transactional
    public void complete(Long userId, Long fileId, long actualSize) {
        FileMetadata file = fileMetaDataRepository.getByIdForUpdateOrThrow(fileId);
        filePolicy.validateCompletion(userId, file.getOwner().getId(), file.getStatus());
        if (file.isCompleted()) {
            return;
        }
        filePolicy.validateActualSize(file.getSize(), actualSize);

        file.completeUpload();
    }
}
