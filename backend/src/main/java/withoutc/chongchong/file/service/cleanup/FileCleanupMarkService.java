package withoutc.chongchong.file.service.cleanup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileCleanupRepository;

@Service
@RequiredArgsConstructor
public class FileCleanupMarkService {

    private final FileCleanupRepository fileCleanupRepository;

    @Transactional(readOnly = true)
    public List<Long> findCandidateIds(long afterId, OrphanCutoffs cutoffs, int limit) {
        return fileCleanupRepository.findOrphanCandidateIds(
                afterId, cutoffs.pending(), cutoffs.unreferenced(), limit);
    }

    @Transactional
    public MarkResult markBatch(List<Long> candidateIds, OrphanCutoffs cutoffs) {
        List<FileMetadata> locked = fileCleanupRepository.lockByIdsSkipLocked(candidateIds);
        List<Long> lockedIds = locked.stream().map(FileMetadata::getId).toList();
        Set<Long> orphanIds = fileCleanupRepository.findOrphanIdsAmong(
                lockedIds, cutoffs.pending(), cutoffs.unreferenced());

        int pendingExpired = 0;
        int unreferenced = 0;
        for (FileMetadata file : locked) {
            if (!orphanIds.contains(file.getId())) {
                continue;
            }
            if (file.getStatus() == FileUploadStatus.PENDING) {
                pendingExpired++;
            } else {
                unreferenced++;
            }
            file.markForDeletion();
        }
        int skipped = candidateIds.size() - pendingExpired - unreferenced;
        return new MarkResult(pendingExpired, unreferenced, skipped);
    }

    public record OrphanCutoffs(LocalDateTime pending, LocalDateTime unreferenced) {
    }

    public record MarkResult(int pendingExpired, int unreferenced, int skipped) {
    }
}
