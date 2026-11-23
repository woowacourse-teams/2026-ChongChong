package withoutc.chongchong.assignment.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentListResponse(
        Long nextCursor,
        boolean hasNext,
        LocalDateTime asOf,
        List<AssignmentSummaryResponse> assignments
) {
    public static AssignmentListResponse of(
            Long nextCursor,
            boolean hasNext,
            LocalDateTime asOf,
            List<AssignmentSummaryResponse> assignments
    ) {
        return new AssignmentListResponse(nextCursor, hasNext, asOf, assignments);
    }
}
