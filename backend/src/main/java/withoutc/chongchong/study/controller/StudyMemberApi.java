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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.study.dto.StudyInviteTokenRequest;
import withoutc.chongchong.study.dto.StudyMemberJoinResponse;
import withoutc.chongchong.study.dto.StudyMembersResponse;

@Tag(name = "Study Member", description = "스터디 멤버 API")
@SecurityRequirement(name = "bearerAuth")
public interface StudyMemberApi {

    @Operation(summary = "스터디 참여", description = "초대 토큰을 사용해 스터디에 참여한다.")
    @ApiResponse(
            responseCode = "201",
            description = "스터디 참여 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StudyMemberJoinResponse.class)
            )
    )
    ResponseEntity<StudyMemberJoinResponse> join(
            AuthenticatedUser user,
            @Valid StudyInviteTokenRequest request
    );

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
    ResponseEntity<StudyMembersResponse> getAllStudyMembers(
            AuthenticatedUser user,
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            @Parameter(description = "스터디 ID", example = "1")
            Long studyId
    );

    @Operation(
            operationId = "expel",
            summary = "스터디 멤버 방출",
            description = "스터디 리더가 일반 멤버를 방출한다."
    )
    @ApiResponse(responseCode = "204", description = "스터디 멤버 방출 성공")
    ResponseEntity<Void> expel(
            AuthenticatedUser user,
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            @Parameter(description = "스터디 ID", example = "1")
            Long studyId,
            @Positive(message = "스터디 멤버 ID는 양수여야 합니다.")
            @Parameter(description = "방출할 스터디 멤버 ID", example = "2")
            Long memberId
    );

    @Operation(
            operationId = "leave",
            summary = "스터디 탈퇴",
            description = "현재 사용자가 스터디에서 탈퇴한다. 리더는 탈퇴할 수 없다."
    )
    @ApiResponse(responseCode = "204", description = "스터디 탈퇴 성공")
    ResponseEntity<Void> leave(
            AuthenticatedUser user,
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            @Parameter(description = "스터디 ID", example = "1")
            Long studyId
    );
}
