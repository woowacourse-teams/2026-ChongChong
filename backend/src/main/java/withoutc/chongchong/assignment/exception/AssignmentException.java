package withoutc.chongchong.assignment.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class AssignmentException extends BusinessException {
    public AssignmentException(AssignmentErrorCode errorCode) {
        super(errorCode);
    }
}
