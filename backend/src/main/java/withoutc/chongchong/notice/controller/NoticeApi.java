package withoutc.chongchong.notice.controller;

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
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.controller.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.controller.dto.NoticeDetailResponse;
import withoutc.chongchong.notice.controller.dto.NoticeListResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusResponse;
import withoutc.chongchong.notice.controller.dto.NoticeStatusesResponse;
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;

@Tag(name = "Notice", description = "공지 API")
@SecurityRequirement(name = "bearerAuth")
public interface NoticeApi {

    @Operation(summary = "공지 생성", description = "스터디 리더가 공지를 생성한다.")
    @ApiResponse(responseCode = "201", description = "공지 생성 성공")
    ResponseEntity<NoticeCreateResponse> createNotice(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Valid NoticeCreateRequest request
    );

    @Operation(summary = "공지 상세 조회", description = "스터디 공지의 상세 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "공지 상세 조회 성공")
    ResponseEntity<NoticeDetailResponse> getNoticeDetail(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "공지 ID", example = "1") Long noticeId
    );

    @Operation(summary = "공지 목록 조회", description = "사용자의 역할에 맞는 공지 목록을 조회한다.")
    @ApiResponse(
            responseCode = "200",
            description = "공지 목록 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = NoticeListResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "Leader",
                                    summary = "스터디 리더 응답",
                                    value = NoticeApiExamples.LEADER_NOTICE_LIST_RESPONSE
                            ),
                            @ExampleObject(
                                    name = "Member",
                                    summary = "스터디 멤버 응답",
                                    value = NoticeApiExamples.MEMBER_NOTICE_LIST_RESPONSE
                            )
                    }
            )
    )
    ResponseEntity<NoticeListResponse> getNotices(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1")
            Long studyId,
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

    @Operation(summary = "공지 읽음 상태 목록 조회", description = "스터디 리더가 공지를 읽은 멤버와 읽지 않은 멤버를 조회한다.")
    @ApiResponse(responseCode = "200", description = "공지 읽음 상태 목록 조회 성공")
    ResponseEntity<NoticeStatusesResponse> getAllReadStatuses(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "공지 ID", example = "1") Long noticeId
    );

    @Operation(summary = "공지 수정", description = "스터디 리더가 공지를 수정한다.")
    @ApiResponse(responseCode = "204", description = "공지 수정 성공")
    ResponseEntity<Void> updateNotice(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "공지 ID", example = "1") Long noticeId,
            @Valid NoticeUpdateRequest request
    );

    @Operation(summary = "공지 삭제", description = "스터디 리더가 공지를 삭제한다.")
    @ApiResponse(responseCode = "204", description = "공지 삭제 성공")
    ResponseEntity<Void> deleteNotice(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "공지 ID", example = "1") Long noticeId
    );

    @Operation(summary = "공지 읽음 처리", description = "현재 사용자가 공지를 읽음 처리한다.")
    @ApiResponse(responseCode = "200", description = "공지 읽음 처리 성공")
    ResponseEntity<NoticeReadResponse> readNotice(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "공지 ID", example = "1") Long noticeId
    );

    @Operation(summary = "내 공지 읽음 상태 조회", description = "현재 사용자의 공지 읽음 상태를 조회한다.")
    @ApiResponse(responseCode = "200", description = "내 공지 읽음 상태 조회 성공")
    ResponseEntity<NoticeReadStatusResponse> getMyReadStatus(
            AuthenticatedUser currentUser,
            @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @Parameter(description = "공지 ID", example = "1") Long noticeId
    );
}
