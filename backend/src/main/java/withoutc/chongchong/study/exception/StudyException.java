package withoutc.chongchong.study.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class StudyException extends BusinessException {

    public StudyException(StudyErrorCode errorCode) {
        super(errorCode);
    }
}
