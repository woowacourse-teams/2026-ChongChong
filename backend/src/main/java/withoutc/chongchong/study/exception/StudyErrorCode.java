package withoutc.chongchong.study.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StudyErrorCode implements ErrorCode {

    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND", "존재하지 않는 스터디입니다."),

    INVALID_STUDY_ID(HttpStatus.BAD_REQUEST, "INVALID_STUDY_ID", "유효하지 않은 스터디 ID입니다."),

    INVALID_INVITE_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_INVITE_TOKEN", "유효하지 않은 초대 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
