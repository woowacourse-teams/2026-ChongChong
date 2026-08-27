package withoutc.chongchong.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PushTokenErrorCode implements ErrorCode {

    PUSH_TOKEN_ALREADY_EXISTS(HttpStatus.CONFLICT, "PUSH_TOKEN_ALREADY_EXISTS", "같은 푸시 토큰 제공자는 동일한 토큰을 생성할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
