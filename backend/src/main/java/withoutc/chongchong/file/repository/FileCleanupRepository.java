package withoutc.chongchong.file.repository;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;

@Repository
@RequiredArgsConstructor
public class FileCleanupRepository {

    private static final String ORPHAN_CONDITION = """
            f.status IN ('PENDING', 'COMPLETED')
              AND (
                  (f.status = 'PENDING' AND f.created_at < :pendingCutoff)
                  OR (f.status = 'COMPLETED' AND f.created_at < :unreferencedCutoff)
              )
              AND %s
            """.formatted(FileReferences.notReferencedCondition("f"));

    private static final String FIND_CANDIDATE_IDS = """
            SELECT f.id
            FROM file_metadata f
            WHERE f.id > :afterId
              AND %s
            ORDER BY f.id
            LIMIT :limit
            """.formatted(ORPHAN_CONDITION);

    private static final String LOCK_BY_IDS = """
            SELECT *
            FROM file_metadata
            WHERE id IN (:ids)
            ORDER BY id
            FOR UPDATE SKIP LOCKED
            """;

    private static final String FIND_ORPHAN_IDS_AMONG = """
            SELECT f.id
            FROM file_metadata f
            WHERE f.id IN (:ids)
              AND %s
            """.formatted(ORPHAN_CONDITION);

    private static final String CLAIM_DELETABLE = """
            SELECT *
            FROM file_metadata
            WHERE status = 'DELETE_PENDING'
              AND delete_attempts < :maxAttempts
              AND (delete_claimed_at IS NULL OR delete_claimed_at <= :leaseCutoff)
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """;

    private static final String DELETE_PENDING_ROWS = """
            DELETE FROM file_metadata
            WHERE id IN (:ids)
              AND status = 'DELETE_PENDING'
            """;

    private static final String BACKLOG_SNAPSHOT = """
            SELECT
                COUNT(*) FILTER (WHERE delete_attempts < :maxAttempts),
                COUNT(*) FILTER (WHERE delete_attempts >= :maxAttempts)
            FROM file_metadata
            WHERE status = 'DELETE_PENDING'
            """;

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Long> findOrphanCandidateIds(
            long afterId,
            LocalDateTime pendingCutoff,
            LocalDateTime unreferencedCutoff,
            int limit
    ) {
        List<Number> ids = entityManager.createNativeQuery(FIND_CANDIDATE_IDS)
                .setParameter("afterId", afterId)
                .setParameter("pendingCutoff", pendingCutoff)
                .setParameter("unreferencedCutoff", unreferencedCutoff)
                .setParameter("limit", limit)
                .getResultList();
        return ids.stream().map(Number::longValue).toList();
    }

    @SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.MANDATORY)
    public List<FileMetadata> lockByIdsSkipLocked(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createNativeQuery(LOCK_BY_IDS, FileMetadata.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.MANDATORY)
    public Set<Long> findOrphanIdsAmong(
            List<Long> lockedIds,
            LocalDateTime pendingCutoff,
            LocalDateTime unreferencedCutoff
    ) {
        if (lockedIds.isEmpty()) {
            return Set.of();
        }
        List<Number> ids = entityManager.createNativeQuery(FIND_ORPHAN_IDS_AMONG)
                .setParameter("ids", lockedIds)
                .setParameter("pendingCutoff", pendingCutoff)
                .setParameter("unreferencedCutoff", unreferencedCutoff)
                .getResultList();
        Set<Long> result = new HashSet<>();
        ids.forEach(id -> result.add(id.longValue()));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.MANDATORY)
    public List<FileMetadata> claimDeletable(LocalDateTime leaseCutoff, int maxAttempts, int limit) {
        return entityManager.createNativeQuery(CLAIM_DELETABLE, FileMetadata.class)
                .setParameter("leaseCutoff", leaseCutoff)
                .setParameter("maxAttempts", maxAttempts)
                .setParameter("limit", limit)
                .getResultList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteDeletePendingRows(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        return entityManager.createNativeQuery(DELETE_PENDING_ROWS)
                .setParameter("ids", ids)
                .executeUpdate();
    }

    public DeletionBacklog findDeletionBacklog(int maxAttempts) {
        Object[] row = (Object[]) entityManager.createNativeQuery(BACKLOG_SNAPSHOT)
                .setParameter("maxAttempts", maxAttempts)
                .getSingleResult();
        return new DeletionBacklog(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }

    public record DeletionBacklog(long retryable, long exhausted) {
    }
}
