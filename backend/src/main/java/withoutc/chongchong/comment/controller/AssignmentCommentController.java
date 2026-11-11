package withoutc.chongchong.comment.controller;

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
import withoutc.chongchong.comment.controller.dto.CommentCreateResponse;
import withoutc.chongchong.comment.controller.dto.CommentListResponse;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.service.CommentService;
import withoutc.chongchong.comment.service.CommentTarget;
import withoutc.chongchong.global.pagination.CursorPageRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/assignments/{assignmentId}/comments")
public class AssignmentCommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentCreateResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive Long studyId,
            @PathVariable @Positive Long assignmentId,
            @Valid @RequestBody CommentWriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                commentService.create(user.id(), studyId, CommentTarget.assignment(assignmentId), request));
    }

    @GetMapping
    public ResponseEntity<CommentListResponse> getList(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive Long studyId,
            @PathVariable @Positive Long assignmentId,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(required = false) @Positive Long focusCommentId,
            @RequestParam(defaultValue = "10") @Positive @Max(CursorPageRequest.MAX_SIZE) int size
    ) {
        return ResponseEntity.ok(commentService.getList(user.id(), studyId, CommentTarget.assignment(assignmentId),
                cursor, focusCommentId, size));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive Long studyId,
            @PathVariable @Positive Long assignmentId,
            @PathVariable @Positive Long commentId,
            @Valid @RequestBody CommentWriteRequest request
    ) {
        commentService.update(user.id(), studyId, CommentTarget.assignment(assignmentId), commentId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive Long studyId,
            @PathVariable @Positive Long assignmentId,
            @PathVariable @Positive Long commentId
    ) {
        commentService.delete(user.id(), studyId, CommentTarget.assignment(assignmentId), commentId);
        return ResponseEntity.noContent().build();
    }
}
