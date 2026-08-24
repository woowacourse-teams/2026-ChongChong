package withoutc.chongchong.assignment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum AssignmentErrorCode implements ErrorCode {

    INVALID_TITLE(HttpStatus.BAD_REQUEST, "INVALID_TITLE", "과제 제목이 올바르지 않습니다."),

    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "INVALID_CONTENT", "과제 내용이 올바르지 않습니다."),

    INVALID_REMIND_AT(HttpStatus.BAD_REQUEST, "INVALID_CLOSE_AT", "과제 마감 시각이 올바르지 않습니다."),

    ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ASSIGNMENT_NOT_FOUND", "존재하지 않는 과제입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
