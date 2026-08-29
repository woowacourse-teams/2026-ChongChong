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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
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
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.global.pagination.CursorPageRequest;

@Tag(name = "Assignment", description = "과제 API")
@SecurityRequirement(name = "bearerAuth")
public interface AssignmentApi {

    @Operation(summary = "과제 생성", description = "스터디 리더가 과제를 생성한다.")
    @ApiResponse(responseCode = "201", description = "과제 생성 성공")
    ResponseEntity<AssignmentCreateResponse> createAssignment(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Valid AssignmentCreateRequest request
    );

    @Operation(summary = "과제 상세 조회", description = "스터디 과제의 상세 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 상세 조회 성공")
    ResponseEntity<AssignmentDetailResponse> getAssignmentDetail(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId
    );

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
    ResponseEntity<AssignmentListResponse> getAssignments(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @RequestParam(required = false)
            @Parameter(description = "다음 페이지 조회 커서", example = "10")
            @Positive(message = "cursor는 양수여야 합니다.")
            Long cursor,
            @RequestParam(defaultValue = "10")
            @Parameter(description = "페이지 크기", example = "10", schema = @Schema(defaultValue = "10"))
            @Positive(message = "size는 양수여야 합니다.")
            @Max(value = CursorPageRequest.MAX_SIZE, message = "size는 100 이하여야 합니다.")
            int size
    );

    @Operation(summary = "과제 수정", description = "스터디 리더가 과제를 수정한다.")
    @ApiResponse(responseCode = "204", description = "과제 수정 성공")
    ResponseEntity<Void> updateAssignment(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId,
            @Valid AssignmentUpdateRequest request
    );

    @Operation(summary = "과제 삭제", description = "스터디 리더가 과제를 삭제한다.")
    @ApiResponse(responseCode = "204", description = "과제 삭제 성공")
    ResponseEntity<Void> deleteAssignment(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId
    );

    @Operation(summary = "과제 제출", description = "현재 사용자가 과제를 제출한다.")
    @ApiResponse(responseCode = "201", description = "과제 제출 성공")
    ResponseEntity<AssignmentSubmitResponse> submitAssignment(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId,
            @Valid AssignmentSubmitRequest request
    );

    @Operation(summary = "과제 제출 수정", description = "현재 사용자의 과제 제출 내용을 수정한다.")
    @ApiResponse(responseCode = "204", description = "과제 제출 수정 성공")
    ResponseEntity<Void> updateSubmission(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId,
            @Parameter(description = "제출 ID", example = "1") Long submissionId,
            @Valid AssignmentSubmitRequest request
    );

    @Operation(summary = "과제 제출 상태 목록 조회", description = "스터디 리더가 멤버별 과제 제출 상태를 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 제출 상태 목록 조회 성공")
    ResponseEntity<AssignmentStatusesResponse> getAllSubmissionStatuses(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId
    );

    @Operation(summary = "과제 제출 상세 조회", description = "과제 제출의 상세 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 제출 상세 조회 성공")
    ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId,
            @Parameter(description = "제출 ID", example = "1") Long submissionId
    );

    @Operation(summary = "과제 제출 목록 조회", description = "스터디 리더가 과제 제출 목록을 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 제출 목록 조회 성공")
    ResponseEntity<SubmissionListResponse> getSubmissionList(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId
    );
}
