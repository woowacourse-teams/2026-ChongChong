package withoutc.chongchong.notification.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class PushTokenException extends BusinessException {

    public PushTokenException(PushTokenErrorCode errorCode) {
        super(errorCode);
    }
}
