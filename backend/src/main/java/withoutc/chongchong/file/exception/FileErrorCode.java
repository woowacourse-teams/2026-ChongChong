package withoutc.chongchong.file.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    INVALID_FILE_OWNER(HttpStatus.BAD_REQUEST, "INVALID_FILE_OWNER", "파일 소유자가 올바르지 않습니다."),
    INVALID_ORIGINAL_FILE_NAME(HttpStatus.BAD_REQUEST, "INVALID_ORIGINAL_FILE_NAME", "원본 파일명이 올바르지 않습니다."),
    INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST, "INVALID_OBJECT_KEY", "파일 저장 키가 올바르지 않습니다."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "INVALID_CONTENT_TYPE", "파일 유형이 올바르지 않습니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "INVALID_FILE_SIZE", "파일 크기가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
