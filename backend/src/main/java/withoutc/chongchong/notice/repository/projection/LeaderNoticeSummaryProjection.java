package withoutc.chongchong.notice.repository.projection;

public record LeaderNoticeSummaryProjection(
        Long id,
        String title,
        long completeCount
) {
}
