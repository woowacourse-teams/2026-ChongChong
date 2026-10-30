package withoutc.chongchong.comment.controller;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.global.exception.response.ErrorResponse;
import withoutc.chongchong.global.logging.RequestLoggingContext;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = NoticeCommentController.class)
public class CommentExceptionHandler {

    @ExceptionHandler({PessimisticLockingFailureException.class, LockTimeoutException.class,
            PessimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleLockFailure(HttpServletRequest request) {
        CommentErrorCode error = CommentErrorCode.STATE_CHANGED;
        RequestLoggingContext.recordErrorCode(request, error);
        return ResponseEntity.status(error.getHttpStatus()).body(ErrorResponse.from(error));
    }
}
