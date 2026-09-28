package withoutc.chongchong.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum WebPushErrorCode implements ErrorCode {

    INVALID_WEB_PUSH_CONFIG(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_WEB_PUSH_CONFIG", "Web Push 설정을 초기화할 수 없습니다."),

    INVALID_WEB_PUSH_SUBSCRIPTION(HttpStatus.BAD_REQUEST, "INVALID_WEB_PUSH_SUBSCRIPTION",
            "웹 푸시 구독의 필수 정보가 올바르지 않습니다."),

    WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED(HttpStatus.CONFLICT, "WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED",
            "다른 사용자에게 등록된 웹 푸시 구독입니다."),

    WEB_PUSH_PAYLOAD_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "WEB_PUSH_PAYLOAD_SERIALIZATION_FAILED", "웹 푸시 payload 생성에 실패했습니다."),

    WEB_PUSH_SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "WEB_PUSH_SUBSCRIPTION_NOT_FOUND", "존재하지 않는 웹 푸시 구독입니다."),

    WEB_PUSH_CRYPTO_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WEB_PUSH_CRYPTO_FAILED", "웹 푸시 암호화 또는 서명에 실패했습니다."),

    WEB_PUSH_TRANSPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WEB_PUSH_TRANSPORT_FAILED", "웹 푸시 서버와 통신에 실패했습니다."),

    WEB_PUSH_SUBSCRIPTION_EXPIRED(HttpStatus.GONE, "WEB_PUSH_SUBSCRIPTION_EXPIRED", "웹 푸시 구독이 만료되었습니다."),

    WEB_PUSH_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "WEB_PUSH_RATE_LIMITED", "웹 푸시 서버 요청이 제한되었습니다."),

    WEB_PUSH_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "WEB_PUSH_PROVIDER_UNAVAILABLE",
            "웹 푸시 서버를 일시적으로 사용할 수 없습니다."),

    WEB_PUSH_PROVIDER_REJECTED(HttpStatus.INTERNAL_SERVER_ERROR, "WEB_PUSH_PROVIDER_REJECTED", "웹 푸시 서버가 요청을 거부했습니다."),

    WEB_PUSH_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "WEB_PUSH_INTERRUPTED", "웹 푸시 발송이 중단되었습니다."),

    UNEXPECTED_WEB_PUSH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_WEB_PUSH_ERROR", "예상치 못한 예외가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
