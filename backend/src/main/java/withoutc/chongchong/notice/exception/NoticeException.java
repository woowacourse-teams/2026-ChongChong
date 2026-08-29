package withoutc.chongchong.notice.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class NoticeException extends BusinessException {
    public NoticeException(NoticeErrorCode errorCode) {
        super(errorCode);
    }
}
