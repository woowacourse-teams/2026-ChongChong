package withoutc.chongchong.notice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements ErrorCode {

    INVALID_TITLE(HttpStatus.BAD_REQUEST, "INVALID_TITLE", "공지 제목이 올바르지 않습니다."),

    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "INVALID_CONTENT", "공지 내용이 올바르지 않습니다."),

    INVALID_REMIND_AT(HttpStatus.BAD_REQUEST, "INVALID_REMIND_AT", "리마인드 시각이 올바르지 않습니다."),

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "존재하지 않는 공지입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
