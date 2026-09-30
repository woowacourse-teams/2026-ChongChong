package withoutc.chongchong.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebPushSendResult {
    SENT(null),
    SUBSCRIPTION_EXPIRED(WebPushErrorCode.WEB_PUSH_SUBSCRIPTION_EXPIRED),
    RATE_LIMITED(WebPushErrorCode.WEB_PUSH_RATE_LIMITED),
    PROVIDER_UNAVAILABLE(WebPushErrorCode.WEB_PUSH_PROVIDER_UNAVAILABLE),
    PERMANENT_FAILURE(WebPushErrorCode.WEB_PUSH_PROVIDER_REJECTED);

    private final WebPushErrorCode errorCode;
}
