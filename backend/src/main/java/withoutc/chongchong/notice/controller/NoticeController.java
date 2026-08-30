package withoutc.chongchong.notice.controller;

import jakarta.validation.Valid;
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
public class NoticeController implements NoticeApi {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeCreateResponse> createNotice(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        NoticeCreateResponse response = noticeService.create(currentUser.id(), studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> getNoticeDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        NoticeDetailResponse response = noticeService.getDetail(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<NoticeListResponse> getNotices(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        NoticeListResponse response = noticeService.getList(currentUser.id(), studyId, cursor, size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{noticeId}/status")
    public ResponseEntity<NoticeStatusesResponse> getAllReadStatuses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        NoticeStatusesResponse response = noticeService.getAllReadStatuses(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{noticeId}")
    public ResponseEntity<Void> updateNotice(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        noticeService.update(currentUser.id(), studyId, noticeId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        noticeService.delete(currentUser.id(), studyId, noticeId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{noticeId}/read")
    public ResponseEntity<NoticeReadResponse> readNotice(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        NoticeReadResponse response = noticeService.markAsRead(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{noticeId}/status/me")
    public ResponseEntity<NoticeReadStatusResponse> getMyReadStatus(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        NoticeReadStatusResponse response = noticeService.getMyReadStatus(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }
}
