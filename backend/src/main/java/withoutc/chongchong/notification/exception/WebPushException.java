package withoutc.chongchong.notification.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class WebPushException extends BusinessException {

    public WebPushException(WebPushErrorCode errorCode) {
        super(errorCode);
    }
}
