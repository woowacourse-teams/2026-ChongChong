package withoutc.chongchong.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<StudyMembersResponse> getAllStudyMembers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        StudyMembersResponse response = studyMemberService.getAllStudyMembers(user.id(), studyId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{studyId}/members/{memberId}")
    public ResponseEntity<Void> expel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId,
            @PathVariable @Positive(message = "스터디 멤버 ID는 양수여야 합니다.") Long memberId
    ) {
        studyMemberService.expel(user.id(), studyId, memberId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{studyId}/members/me")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        studyMemberService.leave(user.id(), studyId);

        return ResponseEntity.noContent().build();
    }
}
