package withoutc.chongchong.notification.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class WebPushSubscriptionException extends BusinessException {

    public WebPushSubscriptionException(WebPushSubscriptionErrorCode errorCode) {
        super(errorCode);
    }
}
