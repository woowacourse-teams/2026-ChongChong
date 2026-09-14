package withoutc.chongchong.notification.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class NotificationException extends BusinessException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
