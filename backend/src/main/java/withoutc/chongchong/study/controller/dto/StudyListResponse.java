package withoutc.chongchong.study.controller.dto;

import java.util.List;

public record StudyListResponse(
        Long nextCursor,
        boolean hasNext,
        List<StudySummaryResponse> studies
) {
}
