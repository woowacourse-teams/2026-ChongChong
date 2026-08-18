package withoutc.chongchong.study.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StudyMemberErrorCode implements ErrorCode {
    STUDY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STUDY_ACCESS_DENIED", "해당 스터디에 대한 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
