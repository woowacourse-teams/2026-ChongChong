package withoutc.chongchong.study.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StudyMemberErrorCode implements ErrorCode {
    STUDY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY_MEMBER_NOT_FOUND", "존재하지 않는 스터디원입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
