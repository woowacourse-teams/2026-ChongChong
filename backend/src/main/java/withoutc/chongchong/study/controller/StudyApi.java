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
import org.springframework.http.ResponseEntity;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.study.controller.dto.LeaderStudyDetailResponse;
import withoutc.chongchong.study.controller.dto.MemberStudyDetailResponse;
import withoutc.chongchong.study.controller.dto.MyStudyListResponse;
import withoutc.chongchong.study.controller.dto.StudyCreateRequest;
import withoutc.chongchong.study.controller.dto.StudyCreateResponse;
import withoutc.chongchong.study.controller.dto.StudyDetailResponse;
import withoutc.chongchong.study.controller.dto.StudyInfoResponse;
import withoutc.chongchong.study.controller.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.controller.dto.StudyUpdateRequest;

@Tag(name = "Study", description = "스터디 API")
@SecurityRequirement(name = "bearerAuth")
public interface StudyApi {

    String LEADER_STUDY_DETAIL_RESPONSE = """
            {
              "notices": {
                "count": 1,
                "items": [
                  {
                    "id": 1,
                    "title": "이번 주 공지",
                    "memberCount": 5,
                    "completeCount": 3
                  }
                ]
              },
              "assignments": {
                "count": 1,
                "items": [
                  {
                    "id": 1,
                    "title": "1주 차 과제",
                    "memberCount": 5,
                    "completeCount": 3
                  }
                ]
              }
            }
            """;

    String MEMBER_STUDY_DETAIL_RESPONSE = """
            {
              "totalCount": 2,
              "notices": {
                "items": [
                  {
                    "id": 1,
                    "title": "이번 주 공지"
                  }
                ]
              },
              "assignments": {
                "items": [
                  {
                    "id": 1,
                    "title": "1주 차 과제"
                  }
                ]
              }
            }
            """;

    @Operation(summary = "스터디 생성", description = "인증된 사용자가 새로운 스터디를 생성한다.")
    @ApiResponse(responseCode = "201", description = "스터디 생성 성공")
    ResponseEntity<StudyCreateResponse> createStudy(
            AuthenticatedUser user,
            @Valid StudyCreateRequest request
    );

    @Operation(summary = "스터디 수정", description = "스터디 리더가 스터디를 수정한다.")
    @ApiResponse(responseCode = "204", description = "스터디 수정 성공")
    ResponseEntity<Void> updateStudy(
            AuthenticatedUser user,
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId,
            @Valid
            StudyUpdateRequest request
    );

    @Operation(summary = "스터디 삭제", description = "스터디 리더가 스터디를 삭제한다.")
    @ApiResponse(responseCode = "204", description = "스터디 삭제 성공")
    ResponseEntity<Void> deleteStudy(
            AuthenticatedUser user,
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    );

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
                            value = LEADER_STUDY_DETAIL_RESPONSE
                    ),
                    @ExampleObject(
                            name = "Member",
                            summary = "스터디 멤버 응답",
                            value = MEMBER_STUDY_DETAIL_RESPONSE
                    )
            })
    )
    ResponseEntity<StudyDetailResponse> getStudyDetail(
            AuthenticatedUser user,
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    );

    @Operation(summary = "스터디 기본 정보 조회", description = "사용자의 스터디 역할과 기본 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "스터디 기본 정보 조회 성공")
    ResponseEntity<StudyInfoResponse> getStudyInfo(
            AuthenticatedUser user,
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    );

    @Operation(summary = "내 스터디 목록 조회", description = "인증된 사용자가 가입한 스터디 목록을 조회한다.")
    @ApiResponse(responseCode = "200", description = "내 스터디 목록 조회 성공")
    ResponseEntity<MyStudyListResponse> getMyStudies(
            AuthenticatedUser user
    );

    @Operation(summary = "스터디 초대 링크 조회", description = "스터디 참여에 사용할 초대 링크를 조회한다.")
    @ApiResponse(responseCode = "200", description = "스터디 초대 링크 조회 성공")
    ResponseEntity<StudyInviteLinkResponse> getInviteLink(
            AuthenticatedUser user,
            @Parameter(description = "스터디 ID", example = "1")
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    );
}
