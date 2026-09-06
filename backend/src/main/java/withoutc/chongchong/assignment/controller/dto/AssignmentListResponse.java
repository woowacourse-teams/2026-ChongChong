package withoutc.chongchong.assignment.controller.dto;

import java.util.List;

public record AssignmentListResponse(
        Long nextCursor,
        boolean hasNext,
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
