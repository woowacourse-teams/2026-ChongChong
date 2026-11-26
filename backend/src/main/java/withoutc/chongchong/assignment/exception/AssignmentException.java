package withoutc.chongchong.assignment.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class AssignmentException extends BusinessException {
    public AssignmentException(AssignmentErrorCode errorCode) {
        super(errorCode);
    }

    public AssignmentException(AssignmentErrorCode errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }
}
