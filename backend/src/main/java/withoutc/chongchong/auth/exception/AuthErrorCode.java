package withoutc.chongchong.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "요청한 작업을 수행할 권한이 없습니다."),

    SOCIAL_AUTHENTICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "SOCIAL_AUTHENTICATION_FAILED",
            "소셜 로그인 인증에 실패했습니다."
    ),

    UNSUPPORTED_SOCIAL_PROVIDER(
            HttpStatus.BAD_REQUEST,
            "UNSUPPORTED_SOCIAL_PROVIDER",
            "지원하지 않는 소셜 로그인 제공자입니다."
    ),

    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "INVALID_USER_ID", "유효하지 않은 사용자 ID입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
