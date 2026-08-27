package withoutc.chongchong.assignment.controller.dto;

import withoutc.chongchong.assignment.entity.Assignment;

public record AssignmentCreateResponse(
        Long assignmentId
) {
    public static AssignmentCreateResponse from(Assignment assignment) {
        return new AssignmentCreateResponse(assignment.getId());
    }
}
