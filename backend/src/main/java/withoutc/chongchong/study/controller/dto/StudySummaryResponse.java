package withoutc.chongchong.study.controller.dto;

public record StudySummaryResponse(
        Long studyId,
        String studyName,
        String description,
        String imageUrl,
        int memberCount
) {
}
