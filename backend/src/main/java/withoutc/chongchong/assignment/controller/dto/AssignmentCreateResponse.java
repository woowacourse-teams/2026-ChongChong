package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import withoutc.chongchong.assignment.entity.Assignment;

public record AssignmentCreateResponse(
        @Schema(description = "생성된 과제 ID", example = "1")
        Long assignmentId
) {
    public static AssignmentCreateResponse from(Assignment assignment) {
        return new AssignmentCreateResponse(assignment.getId());
    }
}
