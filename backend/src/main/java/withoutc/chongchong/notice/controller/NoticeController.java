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
import withoutc.chongchong.notice.service.NoticeService;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/notices")
@RestController
@Tag(name = "Notice", description = "공지 API")
@SecurityRequirement(name = "bearerAuth")
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping
    @Operation(summary = "공지 생성", description = "스터디 리더가 공지를 생성한다.")
    @ApiResponse(responseCode = "201", description = "공지 생성 성공")
    public ResponseEntity<NoticeCreateResponse> createNotice(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                             @PathVariable
                                                             @Parameter(description = "스터디 ID", example = "1")
                                                             Long studyId,
                                                             @Valid @RequestBody NoticeCreateRequest request) {
        NoticeCreateResponse response = noticeService.create(currentUser.id(), studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{noticeId}")
    @Operation(summary = "공지 상세 조회", description = "스터디 공지의 상세 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "공지 상세 조회 성공")
    public ResponseEntity<NoticeDetailResponse> getNoticeDetail(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                @PathVariable
                                                                @Parameter(description = "스터디 ID", example = "1")
                                                                Long studyId,
                                                                @PathVariable
                                                                @Parameter(description = "공지 ID", example = "1")
                                                                Long noticeId) {
        NoticeDetailResponse response = noticeService.getDetail(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
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
    public ResponseEntity<NoticeListResponse> getNotices(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                         @PathVariable
                                                         @Parameter(description = "스터디 ID", example = "1")
                                                         Long studyId,
                                                         @RequestParam(required = false)
                                                         @Parameter(description = "다음 페이지 조회 커서", example = "10")
                                                         @Positive(message = "cursor는 양수여야 합니다.") Long cursor,
                                                         @RequestParam(defaultValue = "10")
                                                         @Parameter(description = "페이지 크기", example = "10", schema = @Schema(defaultValue = "10"))
                                                         @Positive(message = "size는 양수여야 합니다.")
                                                         @Max(value = CursorPageRequest.MAX_SIZE, message = "size는 100 이하여야 합니다.") int size) {
        NoticeListResponse response = noticeService.getList(currentUser.id(), studyId, cursor, size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{noticeId}/status")
    @Operation(summary = "공지 읽음 상태 목록 조회", description = "스터디 리더가 공지를 읽은 멤버와 읽지 않은 멤버를 조회한다.")
    @ApiResponse(responseCode = "200", description = "공지 읽음 상태 목록 조회 성공")
    public ResponseEntity<NoticeStatusesResponse> getAllReadStatuses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "공지 ID", example = "1") Long noticeId) {
        NoticeStatusesResponse response = noticeService.getAllReadStatuses(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{noticeId}")
    @Operation(summary = "공지 수정", description = "스터디 리더가 공지를 수정한다.")
    @ApiResponse(responseCode = "204", description = "공지 수정 성공")
    public ResponseEntity<Void> updateNotice(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
                                             @PathVariable @Parameter(description = "공지 ID", example = "1") Long noticeId,
                                             @Valid @RequestBody NoticeUpdateRequest request) {
        noticeService.update(currentUser.id(), studyId, noticeId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    @Operation(summary = "공지 삭제", description = "스터디 리더가 공지를 삭제한다.")
    @ApiResponse(responseCode = "204", description = "공지 삭제 성공")
    public ResponseEntity<Void> deleteNotice(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
                                             @PathVariable @Parameter(description = "공지 ID", example = "1") Long noticeId) {
        noticeService.delete(currentUser.id(), studyId, noticeId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{noticeId}/read")
    @Operation(summary = "공지 읽음 처리", description = "현재 사용자가 공지를 읽음 처리한다.")
    @ApiResponse(responseCode = "200", description = "공지 읽음 처리 성공")
    public ResponseEntity<NoticeReadResponse> readNotice(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                         @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
                                                         @PathVariable @Parameter(description = "공지 ID", example = "1") Long noticeId) {
        NoticeReadResponse response = noticeService.markAsRead(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{noticeId}/status/me")
    @Operation(summary = "내 공지 읽음 상태 조회", description = "현재 사용자의 공지 읽음 상태를 조회한다.")
    @ApiResponse(responseCode = "200", description = "내 공지 읽음 상태 조회 성공")
    public ResponseEntity<NoticeReadStatusResponse> getMyReadStatus(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Parameter(description = "스터디 ID", example = "1") Long studyId,
            @PathVariable @Parameter(description = "공지 ID", example = "1") Long noticeId) {
        NoticeReadStatusResponse response = noticeService.getMyReadStatus(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }
}
