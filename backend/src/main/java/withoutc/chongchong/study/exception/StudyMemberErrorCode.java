package withoutc.chongchong.study.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StudyMemberErrorCode implements ErrorCode {

    NOT_STUDY_MEMBER(HttpStatus.FORBIDDEN, "NOT_STUDY_MEMBER", "해당 스터디의 멤버가 아닙니다."),

    JOINED_STUDY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "JOINED_STUDY_LIMIT_EXCEEDED",
            "가입할 수 있는 스터디는 최대 50개입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
