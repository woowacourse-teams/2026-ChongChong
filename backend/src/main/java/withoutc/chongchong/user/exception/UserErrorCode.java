package withoutc.chongchong.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    INVALID_USER_NAME(HttpStatus.BAD_REQUEST, "INVALID_USER_NAME", "사용자 이름이 올바르지 않습니다."),

    INVALID_USER_PROFILE_IMAGE_URL(
            HttpStatus.BAD_REQUEST,
            "INVALID_USER_PROFILE_IMAGE_URL",
            "사용자 프로필 이미지 URL이 올바르지 않습니다."
    ),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
