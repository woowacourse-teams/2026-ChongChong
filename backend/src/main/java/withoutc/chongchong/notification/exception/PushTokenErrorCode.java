package withoutc.chongchong.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PushTokenErrorCode implements ErrorCode {

    INVALID_PUSH_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_PUSH_TOKEN", "푸시 토큰의 필수 정보가 올바르지 않습니다."),

    INVALID_INSTALLATION_ID(HttpStatus.BAD_REQUEST, "INVALID_INSTALLATION_ID", "설치된 앱 식별자가 올바르지 않습니다."),

    INVALID_TOKEN_VALUE(HttpStatus.BAD_REQUEST, "INVALID_TOKEN_VALUE", "푸시 토큰 값이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
