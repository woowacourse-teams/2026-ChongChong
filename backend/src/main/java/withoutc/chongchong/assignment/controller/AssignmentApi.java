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
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmissionStatusResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.global.pagination.CursorPageRequest;

@Tag(name = "Assignment", description = "과제 API")
@SecurityRequirement(name = "bearerAuth")
public interface AssignmentApi {

    String LEADER_ASSIGNMENT_LIST_RESPONSE = """
            {
              "nextCursor": null,
              "hasNext": false,
              "assignments": [
                {
                  "id": 1,
                  "title": "1주 차 과제",
                  "content": "이번 주 과제를 제출해주세요.",
                  "submissionMethod": "링크 제출",
                  "closeAt": "2026-08-29T23:59:00",
                  "memberCount": 5,
                  "completeCount": 3,
                  "remindAt": "2026-08-28T10:00:00",
                  "isComplete": false
                }
              ]
            }
            """;

    String MEMBER_ASSIGNMENT_LIST_RESPONSE = """
            {
              "nextCursor": null,
              "hasNext": false,
              "assignments": [
                {
                  "id": 1,
                  "title": "1주 차 과제",
                  "content": "이번 주 과제를 제출해주세요.",
                  "submissionMethod": "링크 제출",
                  "closeAt": "2026-08-29T23:59:00",
                  "isComplete": false
                }
              ]
            }
            """;

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
                                    value = LEADER_ASSIGNMENT_LIST_RESPONSE
                            ),
                            @ExampleObject(
                                    name = "Member",
                                    summary = "스터디 멤버 응답",
                                    value = MEMBER_ASSIGNMENT_LIST_RESPONSE
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

    @Operation(summary = "과제 제출 현황 조회", description = "스터디 리더가 과제의 제출 현황을 조회한다.")
    @ApiResponse(responseCode = "200", description = "과제 제출 현황 조회 성공")
    ResponseEntity<AssignmentSubmissionStatusResponse> getAssignmentSubmissionStatus(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "과제 ID", example = "1") Long assignmentId
    );
}
