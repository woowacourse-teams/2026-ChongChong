package withoutc.chongchong.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum WebPushSubscriptionErrorCode implements ErrorCode {

    INVALID_WEB_PUSH_SUBSCRIPTION(
            HttpStatus.BAD_REQUEST,
            "INVALID_WEB_PUSH_SUBSCRIPTION",
            "웹 푸시 구독의 필수 정보가 올바르지 않습니다."
    ),

    WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED(
            HttpStatus.CONFLICT,
            "WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED",
            "다른 사용자에게 등록된 웹 푸시 구독입니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
