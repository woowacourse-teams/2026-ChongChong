package withoutc.chongchong.study.exception;

import withoutc.chongchong.global.exception.BusinessException;
import withoutc.chongchong.global.exception.code.ErrorCode;

public class StudyMemberException extends BusinessException {
    public StudyMemberException(StudyMemberErrorCode errorCode) {
        super(errorCode);
    }
}
