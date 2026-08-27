package withoutc.chongchong.assignment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import withoutc.chongchong.assignment.controller.dto.AssignmentStatusesResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.service.AssignmentService;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.notice.controller.dto.NoticeStatusesResponse;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/assignments")
@RestController
@Tag(name = "Assignment", description = "과제 API")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {
    private final AssignmentService assignmentService;

    @PostMapping
    @Operation(summary = "과제 생성", description = "스터디 리더가 과제를 생성한다.")
    @ApiResponse(responseCode = "201", description = "과제 생성 성공")
    public ResponseEntity<AssignmentCreateResponse> createAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Valid @RequestBody AssignmentCreateRequest request) {
        AssignmentCreateResponse response = assignmentService.create(currentUser.id(), studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{assignmentId}")
    @Operation(summary = "과제 상세 조회", description = "스터디 과제의 상세 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 상세 조회 성공")
    public ResponseEntity<AssignmentDetailResponse> getAssignmentDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId) {
        AssignmentDetailResponse response = assignmentService.getDetail(currentUser.id(), studyId, assignmentId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "과제 목록 조회", description = "사용자의 역할에 맞는 과제 목록을 조회한다.")
    @ApiResponse(
            responseCode = "200",
            description = "과제 목록 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = AssignmentListResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "Leader",
                                    summary = "스터디 리더 응답",
                                    value = AssignmentApiExamples.LEADER_ASSIGNMENT_LIST_RESPONSE
                            ),
                            @ExampleObject(
                                    name = "Member",
                                    summary = "스터디 멤버 응답",
                                    value = AssignmentApiExamples.MEMBER_ASSIGNMENT_LIST_RESPONSE
                            )
                    }
            )
    )
    public ResponseEntity<AssignmentListResponse> getAssignments(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                 @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
                                                                 @RequestParam(required = false)
                                                                 @Parameter(description = "다음 페이지 조회 커서", example = "10")
                                                                 @Positive(message = "cursor는 양수여야 합니다.") Long cursor,
                                                                 @RequestParam(defaultValue = "10")
                                                                 @Parameter(description = "페이지 크기", example = "10", schema = @Schema(defaultValue = "10"))
                                                                 @Positive(message = "size는 양수여야 합니다.")
                                                                 @Max(value = CursorPageRequest.MAX_SIZE, message = "size는 100 이하여야 합니다.") int size) {
        AssignmentListResponse response = assignmentService.getList(currentUser.id(), studyId, cursor, size);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{assignmentId}")
    @Operation(summary = "과제 수정", description = "스터디 리더가 과제를 수정한다.")
    @ApiResponse(responseCode = "204", description = "과제 수정 성공")
    public ResponseEntity<Void> updateAssignment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                 @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
                                                 @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId,
                                                 @Valid @RequestBody AssignmentUpdateRequest request) {
        assignmentService.update(currentUser.id(), studyId, assignmentId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{assignmentId}")
    @Operation(summary = "과제 삭제", description = "스터디 리더가 과제를 삭제한다.")
    @ApiResponse(responseCode = "204", description = "과제 삭제 성공")
    public ResponseEntity<Void> deleteAssignment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                 @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
                                                 @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId) {
        assignmentService.delete(currentUser.id(), studyId, assignmentId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{assignmentId}/submissions")
    @Operation(summary = "과제 제출", description = "현재 사용자가 과제를 제출한다.")
    @ApiResponse(responseCode = "201", description = "과제 제출 성공")
    public ResponseEntity<AssignmentSubmitResponse> submitAssignment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId,
            @Valid @RequestBody AssignmentSubmitRequest request) {
        AssignmentSubmitResponse response = assignmentService.submitAssignment(currentUser.id(), studyId, assignmentId,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{assignmentId}/submissions/{submissionId}")
    @Operation(summary = "과제 제출 수정", description = "현재 사용자의 과제 제출 내용을 수정한다.")
    @ApiResponse(responseCode = "204", description = "과제 제출 수정 성공")
    public ResponseEntity<Void> updateSubmission(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId,
            @PathVariable @Parameter(description = "제출 ID", example = "1") Long submissionId,
            @Valid @RequestBody AssignmentSubmitRequest request) {
        assignmentService.updateSubmission(currentUser.id(), studyId, assignmentId, submissionId, request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assignmentId}/status")
    @Operation(summary = "과제 제출 상태 목록 조회", description = "스터디 리더가 멤버별 과제 제출 상태를 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 제출 상태 목록 조회 성공")
    public ResponseEntity<AssignmentStatusesResponse> getAllSubmissionStatuses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId) {

        AssignmentStatusesResponse response = assignmentService.getAllSubmittedStatus(currentUser.id(), studyId,
                assignmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{assignmentId}/submissions/{submissionId}")
    @Operation(summary = "과제 제출 상세 조회", description = "과제 제출의 상세 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 제출 상세 조회 성공")
    public ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId,
            @PathVariable @Parameter(description = "제출 ID", example = "1") Long submissionId) {
        SubmissionDetailResponse response = assignmentService.getSubmissionDetail(currentUser.id(), studyId,
                assignmentId, submissionId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{assignmentId}/submissions")
    @Operation(summary = "과제 제출 목록 조회", description = "스터디 리더가 과제 제출 목록을 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 제출 목록 조회 성공")
    public ResponseEntity<SubmissionListResponse> getSubmissionList(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "과제 ID", example = "1") Long assignmentId) {
        SubmissionListResponse response = assignmentService.getSubmissionList(currentUser.id(), studyId, assignmentId);

        return ResponseEntity.ok(response);
    }
}
