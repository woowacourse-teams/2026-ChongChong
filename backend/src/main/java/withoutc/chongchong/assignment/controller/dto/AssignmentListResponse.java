package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AssignmentListResponse(
        @Schema(description = "다음 페이지 조회에 사용할 커서", example = "10", nullable = true)
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext,
        @Schema(description = "과제 요약 목록")
        List<AssignmentSummaryResponse> assignments
) {
    public static AssignmentListResponse of(
            Long nextCursor,
            boolean hasNext,
            List<AssignmentSummaryResponse> assignments
    ) {
        return new AssignmentListResponse(nextCursor, hasNext, assignments);
    }
}
