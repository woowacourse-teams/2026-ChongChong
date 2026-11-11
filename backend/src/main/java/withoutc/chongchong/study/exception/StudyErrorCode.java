package withoutc.chongchong.study.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StudyErrorCode implements ErrorCode {

    INVALID_NAME(HttpStatus.BAD_REQUEST, "INVALID_NAME", "스터디 이름이 올바르지 않습니다."),

    INVALID_DESCRIPTION(HttpStatus.BAD_REQUEST, "INVALID_DESCRIPTION", "스터디 설명이 올바르지 않습니다."),

    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND", "존재하지 않는 스터디입니다."),

    STUDY_NOT_PUBLIC(HttpStatus.FORBIDDEN, "STUDY_NOT_PUBLIC", "공개 스터디가 아닙니다."),

    INVALID_STUDY_ID(HttpStatus.BAD_REQUEST, "INVALID_STUDY_ID", "유효하지 않은 스터디 ID입니다."),

    INVALID_IMAGE_ID(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_ID", "첨부 파일 ID가 올바르지 않습니다."),

    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_TYPE", "스터디 프로필에 첨부할 수 없는 이미지 형식입니다."),

    INVALID_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_SIZE", "스터디 이미지 크기가 올바르지 않습니다."),

    INVALID_INVITE_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_INVITE_TOKEN", "유효하지 않거나 만료된 초대 토큰입니다."),

    INVITE_TOKEN_SIGN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INVITE_TOKEN_SIGN_FAILED", "초대 토큰 서명에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
