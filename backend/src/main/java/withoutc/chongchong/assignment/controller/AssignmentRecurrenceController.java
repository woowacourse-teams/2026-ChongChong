package withoutc.chongchong.assignment.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceListResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceUpdateRequest;
import withoutc.chongchong.assignment.service.AssignmentRecurrenceService;
import withoutc.chongchong.auth.security.AuthenticatedUser;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/assignment-recurrences")
@RestController
public class AssignmentRecurrenceController {
    private final AssignmentRecurrenceService assignmentRecurrenceService;

    @PostMapping
    public ResponseEntity<AssignmentRecurrenceCreateResponse> createAssignmentRecurrence(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @Valid @RequestBody AssignmentRecurrenceCreateRequest request
    ) {
        AssignmentRecurrenceCreateResponse response =
                assignmentRecurrenceService.create(currentUser.id(), studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<AssignmentRecurrenceListResponse> getAssignmentRecurrences(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId
    ) {
        AssignmentRecurrenceListResponse response = assignmentRecurrenceService.getList(currentUser.id(), studyId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{recurrenceId}")
    public ResponseEntity<Void> updateAssignmentRecurrence(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long recurrenceId,
            @Valid @RequestBody AssignmentRecurrenceUpdateRequest request
    ) {
        assignmentRecurrenceService.update(currentUser.id(), studyId, recurrenceId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{recurrenceId}")
    public ResponseEntity<Void> deleteAssignmentRecurrence(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long recurrenceId
    ) {
        assignmentRecurrenceService.delete(currentUser.id(), studyId, recurrenceId);

        return ResponseEntity.noContent().build();
    }
}
