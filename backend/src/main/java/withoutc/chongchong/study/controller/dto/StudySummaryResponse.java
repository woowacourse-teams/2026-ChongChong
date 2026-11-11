package withoutc.chongchong.study.controller.dto;

public record StudySummaryResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        int memberCount
) {
}
