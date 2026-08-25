package withoutc.chongchong.assignment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.service.AssignmentService;
import withoutc.chongchong.auth.security.AuthenticatedUser;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/assignments")
@RestController
public class AssignmentController {
    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<AssignmentCreateResponse> createAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @Valid @RequestBody AssignmentCreateRequest request) {
        AssignmentCreateResponse response = assignmentService.create(currentUser.id(), studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("{assignmentId}")
    public ResponseEntity<Void> updateAssignment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                 @PathVariable Long studyId, @PathVariable Long assignmentId,
                                                 @Valid AssignmentUpdateRequest request
    ) {
        assignmentService.update(currentUser.id(), studyId, assignmentId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                 @PathVariable Long studyId, @PathVariable Long assignmentId) {
        assignmentService.delete(currentUser.id(), studyId, assignmentId);

        return ResponseEntity.noContent().build();
    }
}
