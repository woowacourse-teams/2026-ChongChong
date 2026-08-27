package withoutc.chongchong.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
import withoutc.chongchong.study.dto.LeaderStudyDetailResponse;
import withoutc.chongchong.study.dto.MemberStudyDetailResponse;
import withoutc.chongchong.study.dto.MyStudyListResponse;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyCreateResponse;
import withoutc.chongchong.study.dto.StudyDetailResponse;
import withoutc.chongchong.study.dto.StudyInfoResponse;
import withoutc.chongchong.study.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.service.StudyService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies")
@Tag(name = "Study", description = "스터디 API")
@SecurityRequirement(name = "bearerAuth")
public class StudyController {

    private final StudyService studyService;

    @PostMapping
    @Operation(summary = "스터디 생성", description = "인증된 사용자가 새로운 스터디를 생성한다.")
    @ApiResponse(responseCode = "201", description = "스터디 생성 성공")
    public ResponseEntity<StudyCreateResponse> createStudy(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid StudyCreateRequest request
    ) {
        StudyCreateResponse response = studyService.createStudy(user.id(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{studyId}")
    @Operation(summary = "스터디 상세 조회", description = "사용자의 역할에 맞는 스터디 상세 정보를 조회한다.")
    @ApiResponse(
            responseCode = "200",
            description = "스터디 상세 조회 성공",
            content = @Content(schema = @Schema(
                    oneOf = {LeaderStudyDetailResponse.class, MemberStudyDetailResponse.class}
            ), examples = {
                    @ExampleObject(
                            name = "Leader",
                            summary = "스터디 리더 응답",
                            value = StudyApiExamples.LEADER_STUDY_DETAIL_RESPONSE
                    ),
                    @ExampleObject(
                            name = "Member",
                            summary = "스터디 멤버 응답",
                            value = StudyApiExamples.MEMBER_STUDY_DETAIL_RESPONSE
                    )
            })
    )
    public ResponseEntity<StudyDetailResponse> getStudyDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        StudyDetailResponse response = studyService.getStudyDetail(user.id(), studyId);
        return ResponseEntity
                .ok(response);
    }

    @DeleteMapping("/{studyId}")
    @Operation(summary = "스터디 삭제", description = "스터디 리더가 스터디를 삭제한다.")
    @ApiResponse(responseCode = "204", description = "스터디 삭제 성공")
    public ResponseEntity<Void> deleteStudy(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        studyService.deleteStudy(user.id(), studyId);
        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/{studyId}/info")
    @Operation(summary = "스터디 기본 정보 조회", description = "사용자의 스터디 역할과 기본 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "스터디 기본 정보 조회 성공")
    public ResponseEntity<StudyInfoResponse> getStudyInfo(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        StudyInfoResponse response = studyService.getStudyInfo(user.id(), studyId);
        return ResponseEntity
                .ok(response);
    }


    @GetMapping("/me")
    @Operation(summary = "내 스터디 목록 조회", description = "인증된 사용자가 가입한 스터디 목록을 조회한다.")
    @ApiResponse(responseCode = "200", description = "내 스터디 목록 조회 성공")
    public ResponseEntity<MyStudyListResponse> getMyStudies(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MyStudyListResponse response = studyService.getMyStudies(user.id());

        return ResponseEntity
                .ok(response);
    }

    @GetMapping("/{studyId}/invite-link")
    @Operation(summary = "스터디 초대 링크 조회", description = "스터디 참여에 사용할 초대 링크를 조회한다.")
    @ApiResponse(responseCode = "200", description = "스터디 초대 링크 조회 성공")
    public ResponseEntity<StudyInviteLinkResponse> getInviteLink(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        StudyInviteLinkResponse response = studyService.getInviteLink(user.id(), studyId);

        return ResponseEntity
                .ok(response);
    }
}
