package withoutc.chongchong.study.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class StudyMemberException extends BusinessException {
    public StudyMemberException(StudyMemberErrorCode errorCode) {
        super(errorCode);
    }
}
