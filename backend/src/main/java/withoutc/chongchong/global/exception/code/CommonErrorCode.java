package withoutc.chongchong.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),

    INVALID_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_PARAMETER", "요청 파라미터가 올바르지 않습니다."),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 잘못되었습니다."),

    UNSUPPORTED_HTTP_METHOD(HttpStatus.METHOD_NOT_ALLOWED, "UNSUPPORTED_HTTP_METHOD", "지원하지 않는 HTTP 메서드입니다."),

    UNSUPPORTED_PATH(HttpStatus.NOT_FOUND, "UNSUPPORTED_PATH", "존재하지 않는 경로입니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
