package withoutc.chongchong.study.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StudyMemberErrorCode implements ErrorCode {
    STUDY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STUDY_ACCESS_DENIED", "해당 스터디에 대한 접근 권한이 없습니다."),

    NOT_STUDY_LEADER(HttpStatus.FORBIDDEN, "NOT_STUDY_LEADER", "스터디 리더만 수행할 수 있습니다."),

    STUDY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY_MEMBER_NOT_FOUND", "존재하지 않는 스터디 멤버입니다."),

    STUDY_LEADER_CANNOT_BE_REMOVED(HttpStatus.FORBIDDEN, "STUDY_LEADER_CANNOT_BE_REMOVED", "스터디 리더는 방출할 수 없습니다."),

    STUDY_LEADER_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "STUDY_LEADER_CANNOT_LEAVE", "스터디 리더는 탈퇴할 수 없습니다."),

    JOINED_STUDY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "JOINED_STUDY_LIMIT_EXCEEDED", "가입할 수 있는 스터디는 최대 50개입니다."),

    ALREADY_JOINED_STUDY(HttpStatus.CONFLICT, "ALREADY_JOINED_STUDY", "해당 스터디에 이미 가입되어 있습니다."),

    STUDY_MEMBER_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "STUDY_MEMBER_LIMIT_EXCEEDED", "스터디 정원이 가득 찼습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
