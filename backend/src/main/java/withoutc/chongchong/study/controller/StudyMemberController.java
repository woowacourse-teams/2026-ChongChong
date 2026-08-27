package withoutc.chongchong.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.study.dto.StudyInviteTokenRequest;
import withoutc.chongchong.study.dto.StudyMembersResponse;
import withoutc.chongchong.study.service.StudyMemberService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies")
@Tag(name = "Study Member", description = "스터디 멤버 API")
@SecurityRequirement(name = "bearerAuth")
public class StudyMemberController {

    private final StudyMemberService studyMemberService;

    @PostMapping("/join")
    @Operation(summary = "스터디 참여", description = "초대 토큰을 사용해 스터디에 참여한다.")
    @ApiResponse(responseCode = "201", description = "스터디 참여 성공")
    public ResponseEntity<Void> join(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid StudyInviteTokenRequest request
    ) {
        studyMemberService.join(user.id(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping("/{studyId}/members")
    @Operation(
            operationId = "getAllStudyMembers",
            summary = "스터디 멤버 목록 조회",
            description = "스터디에 가입한 멤버 목록을 조회한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "스터디 멤버 목록 조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StudyMembersResponse.class)
            )
    )
    public ResponseEntity<StudyMembersResponse> getAllStudyMembers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        StudyMembersResponse response = studyMemberService.getAllStudyMembers(user.id(), studyId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{studyId}/members/{memberId}")
    @Operation(
            operationId = "expel",
            summary = "스터디 멤버 방출",
            description = "스터디 리더가 일반 멤버를 방출한다."
    )
    @ApiResponse(responseCode = "204", description = "스터디 멤버 방출 성공")
    public ResponseEntity<Void> expel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId,
            @PathVariable
            @Parameter(description = "방출할 스터디 멤버 ID", example = "2")
            @Positive(message = "스터디 멤버 ID는 양수여야 합니다.") Long memberId
    ) {
        studyMemberService.expel(user.id(), studyId, memberId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{studyId}/members/me")
    @Operation(
            operationId = "leave",
            summary = "스터디 탈퇴",
            description = "현재 사용자가 스터디에서 탈퇴한다. 리더는 탈퇴할 수 없다."
    )
    @ApiResponse(responseCode = "204", description = "스터디 탈퇴 성공")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        studyMemberService.leave(user.id(), studyId);

        return ResponseEntity.noContent().build();
    }
}
