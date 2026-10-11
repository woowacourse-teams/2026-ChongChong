package withoutc.chongchong.notice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.notice.controller.dto.NoticeReadResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusesResponse;
import withoutc.chongchong.notice.service.NoticeReadService;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/notices/{noticeId}")
@RestController
public class NoticeReadController {

    private final NoticeReadService noticeReadService;

    @GetMapping("/status")
    public ResponseEntity<NoticeReadStatusesResponse> getAllReadStatuses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        NoticeReadStatusesResponse response = noticeReadService.getAllReadStatuses(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read")
    public ResponseEntity<NoticeReadResponse> readNotice(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        NoticeReadResponse response = noticeReadService.markAsRead(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/me")
    public ResponseEntity<NoticeReadStatusResponse> getMyReadStatus(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long studyId,
            @PathVariable Long noticeId
    ) {
        NoticeReadStatusResponse response = noticeReadService.getMyReadStatus(currentUser.id(), studyId, noticeId);

        return ResponseEntity.ok(response);
    }
}
