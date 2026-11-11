package withoutc.chongchong.comment.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class CommentException extends BusinessException {

    public CommentException(CommentErrorCode errorCode) {
        super(errorCode);
    }
}
