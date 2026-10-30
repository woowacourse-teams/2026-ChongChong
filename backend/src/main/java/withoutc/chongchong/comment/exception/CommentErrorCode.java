package withoutc.chongchong.comment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    INVALID_COMMENT(HttpStatus.BAD_REQUEST, "COMMENT_INVALID_COMMENT", "댓글의 대상이나 작성자가 올바르지 않습니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "COMMENT_INVALID_CONTENT",
            "댓글은 앞뒤 공백을 제거한 뒤 1자 이상 1000자 이하여야 합니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "COMMENT_INVALID_PAGE_REQUEST",
            "cursor와 focusCommentId는 함께 지정할 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "해당 원문의 댓글을 찾을 수 없습니다."),
    STATE_CHANGED(HttpStatus.CONFLICT, "COMMENT_STATE_CHANGED",
            "댓글 대상이나 참여 상태가 변경되었습니다. 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
