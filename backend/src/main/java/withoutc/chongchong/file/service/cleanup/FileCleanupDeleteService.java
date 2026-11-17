package withoutc.chongchong.file.service.cleanup;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileCleanupRepository;
import withoutc.chongchong.file.repository.FileCleanupRepository.DeletionBacklog;

@Service
@RequiredArgsConstructor
public class FileCleanupDeleteService {

    private final FileCleanupRepository fileCleanupRepository;

    @Transactional
    public List<DeletionTarget> claim(LocalDateTime now, LocalDateTime leaseCutoff, int maxAttempts, int limit) {
        List<FileMetadata> files = fileCleanupRepository.claimDeletable(leaseCutoff, maxAttempts, limit);
        files.forEach(file -> file.claimDeletion(now));
        return files.stream()
                .map(file -> new DeletionTarget(file.getId(), file.getObjectKey(), file.getDeleteAttempts()))
                .toList();
    }

    @Transactional
    public int deleteRows(List<Long> ids) {
        return fileCleanupRepository.deleteDeletePendingRows(ids);
    }

    @Transactional(readOnly = true)
    public DeletionBacklog findBacklog(int maxAttempts) {
        return fileCleanupRepository.findDeletionBacklog(maxAttempts);
    }

    public record DeletionTarget(Long id, String objectKey, int attempts) {
    }
}
