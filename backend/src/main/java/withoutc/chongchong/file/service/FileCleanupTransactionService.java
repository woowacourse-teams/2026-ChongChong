package withoutc.chongchong.file.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileCleanupRepository;
import withoutc.chongchong.file.repository.FileCleanupRepository.DeletionBacklog;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileCleanupTransactionService {

    private final FileCleanupRepository fileCleanupRepository;

    public List<Long> findCandidateIds(long afterId, OrphanCutoffs cutoffs, int limit) {
        return fileCleanupRepository.findOrphanCandidateIds(
                afterId, cutoffs.pending(), cutoffs.unreferenced(), limit);
    }

    @Transactional
    public MarkResult markBatch(List<Long> candidateIds, OrphanCutoffs cutoffs) {
        List<FileMetadata> locked = fileCleanupRepository.lockByIdsSkipLocked(candidateIds);
        Set<Long> orphanIds = fileCleanupRepository.findOrphanIdsAmong(
                locked.stream().map(FileMetadata::getId).toList(), cutoffs.pending(), cutoffs.unreferenced());

        List<FileMetadata> orphans = locked.stream()
                .filter(file -> orphanIds.contains(file.getId()))
                .toList();
        Map<FileUploadStatus, Long> countByStatus = orphans.stream()
                .collect(Collectors.groupingBy(FileMetadata::getStatus, Collectors.counting()));
        orphans.forEach(FileMetadata::markForDeletion);

        return new MarkResult(
                countByStatus.getOrDefault(FileUploadStatus.PENDING, 0L).intValue(),
                countByStatus.getOrDefault(FileUploadStatus.COMPLETED, 0L).intValue(),
                candidateIds.size() - orphans.size()
        );
    }

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

    public DeletionBacklog findBacklog(int maxAttempts) {
        return fileCleanupRepository.findDeletionBacklog(maxAttempts);
    }

    public record OrphanCutoffs(LocalDateTime pending, LocalDateTime unreferenced) {
    }

    public record MarkResult(int pendingExpired, int unreferenced, int skipped) {
    }

    public record DeletionTarget(Long id, String objectKey, int attempts) {
    }
}
