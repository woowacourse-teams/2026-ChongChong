package withoutc.chongchong.assignment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.assignment.controller.dto.AssignmentStatusesResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.service.AssignmentSubmissionService;
import withoutc.chongchong.auth.security.AuthenticatedUser;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/assignments/{assignmentId}/submissions")
@RestController
public class AssignmentSubmissionController implements AssignmentSubmissionApi {
    private final AssignmentSubmissionService assignmentSubmissionService;

    @PostMapping("")
    public ResponseEntity<AssignmentSubmitResponse> submitAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentSubmitRequest request
    ) {
        AssignmentSubmitResponse response = assignmentSubmissionService.create(currentUser.id(), studyId,
                assignmentId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{submissionId}")
    public ResponseEntity<Void> updateSubmission(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody AssignmentSubmitRequest request
    ) {
        assignmentSubmissionService.update(currentUser.id(), studyId, assignmentId, submissionId, request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<AssignmentStatusesResponse> getAllSubmissionStatuses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long assignmentId
    ) {
        AssignmentStatusesResponse response = assignmentSubmissionService.getAllSubmittedStatus(currentUser.id(),
                studyId, assignmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<MySubmissionDetailResponse> getMySubmissionDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long assignmentId
    ) {
        MySubmissionDetailResponse response = assignmentSubmissionService.getMySubmissionDetail(currentUser.id(),
                studyId, assignmentId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId
    ) {
        SubmissionDetailResponse response = assignmentSubmissionService.getSubmissionDetail(currentUser.id(), studyId,
                assignmentId, submissionId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<SubmissionListResponse> getSubmissionList(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long assignmentId
    ) {
        SubmissionListResponse response = assignmentSubmissionService.getSubmissionList(currentUser.id(), studyId,
                assignmentId);

        return ResponseEntity.ok(response);
    }
}
