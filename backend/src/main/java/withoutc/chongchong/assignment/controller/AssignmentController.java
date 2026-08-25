package withoutc.chongchong.assignment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentDetailResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentListResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.service.AssignmentService;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.global.pagination.CursorPageRequest;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/assignments")
@RestController
public class AssignmentController {
    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<AssignmentCreateResponse> createAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long studyId,
            @Valid @RequestBody AssignmentCreateRequest request) {
        AssignmentCreateResponse response = assignmentService.create(currentUser.id(), studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentDetailResponse> getAssignmentDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long studyId,
            @PathVariable Long assignmentId) {
        AssignmentDetailResponse response = assignmentService.getDetail(currentUser.id(), studyId, assignmentId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<AssignmentListResponse> getAssignments(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @RequestParam(required = false) @Positive(message = "cursor는 양수여야 합니다.") Long cursor,
            @RequestParam(defaultValue = "10") @Positive(message = "size는 양수여야 합니다.")
            @Max(value = CursorPageRequest.MAX_SIZE, message = "size는 100 이하여야 합니다.") int size) {
        AssignmentListResponse response = assignmentService.getList(currentUser.id(), studyId, cursor, size);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("{assignmentId}")
    public ResponseEntity<Void> updateAssignment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                 @PathVariable Long studyId, @PathVariable Long assignmentId,
                                                 @Valid AssignmentUpdateRequest request) {
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
