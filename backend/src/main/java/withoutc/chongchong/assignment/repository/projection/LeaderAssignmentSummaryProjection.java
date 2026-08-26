package withoutc.chongchong.assignment.repository.projection;

public record LeaderAssignmentSummaryProjection(
        Long id,
        String title,
        long memberCount,
        long completeCount
) {
}
