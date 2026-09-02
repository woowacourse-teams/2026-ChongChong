package withoutc.chongchong.assignment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.auth.security.AuthenticatedUser;

@Tag(name = "Assignment Submission", description = "과제 제출물 API")
@SecurityRequirement(name = "bearerAuth")
public interface AssignmentSubmissionApi {

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

    @Operation(summary = "내 과제 제출 조회", description = "현재 사용자의 과제 제출 정보를 조회한다.")
    @ApiResponse(
            responseCode = "200",
            description = "내 과제 제출 조회 성공. 제출 정보가 없으면 빈 본문을 반환한다.",
            content = @Content(schema = @Schema(implementation = MySubmissionDetailResponse.class, nullable = true))
    )
    ResponseEntity<MySubmissionDetailResponse> getMySubmissionDetail(
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
