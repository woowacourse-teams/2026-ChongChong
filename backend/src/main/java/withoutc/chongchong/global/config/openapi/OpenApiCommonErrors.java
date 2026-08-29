package withoutc.chongchong.global.config.openapi;

import java.util.List;
import java.util.Map;

final class OpenApiCommonErrors {

    static final OpenApiError INVALID_INPUT_VALUE = error(
            "400", "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다.",
            Map.of(
                    "code", "INVALID_INPUT_VALUE",
                    "message", "입력값이 올바르지 않습니다.",
                    "errors", List.of(Map.of(
                            "code", "REQUEST_VALIDATION_NOT_BLANK",
                            "field", "title",
                            "reason", "제목은 필수 값입니다."
                    ))
            )
    );
    static final OpenApiError INVALID_REQUEST_PARAMETER = error(
            "400", "INVALID_REQUEST_PARAMETER", "요청 파라미터가 올바르지 않습니다.",
            Map.of(
                    "code", "INVALID_REQUEST_PARAMETER",
                    "message", "요청 파라미터가 올바르지 않습니다.",
                    "errors", List.of(Map.of(
                            "code", "REQUEST_VALIDATION_POSITIVE",
                            "field", "studyId",
                            "reason", "스터디 ID는 양수여야 합니다."
                    ))
            )
    );
    static final OpenApiError INVALID_REQUEST = simple(
            "400", "INVALID_REQUEST", "요청 형식이 잘못되었습니다."
    );
    static final OpenApiError AUTHENTICATION_REQUIRED = simple(
            "401", "AUTHENTICATION_REQUIRED", "인증이 필요합니다."
    );
    static final OpenApiError ACCESS_DENIED = simple(
            "403", "ACCESS_DENIED", "요청한 작업을 수행할 권한이 없습니다."
    );
    static final OpenApiError STUDY_ACCESS_DENIED = simple(
            "403", "STUDY_ACCESS_DENIED", "해당 스터디에 대한 접근 권한이 없습니다."
    );
    static final OpenApiError NOT_STUDY_LEADER = simple(
            "403", "NOT_STUDY_LEADER", "스터디 리더만 수행할 수 있습니다."
    );
    static final OpenApiError INVALID_CSRF_TOKEN = simple(
            "403", "INVALID_CSRF_TOKEN", "유효하지 않은 CSRF Token입니다."
    );
    static final OpenApiError STUDY_NOT_FOUND = simple(
            "404", "STUDY_NOT_FOUND", "존재하지 않는 스터디입니다."
    );
    static final OpenApiError USER_NOT_FOUND = simple(
            "404", "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다."
    );
    static final OpenApiError INTERNAL_SERVER_ERROR = simple(
            "500", "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."
    );

    private OpenApiCommonErrors() {
    }

    private static OpenApiError simple(String responseCode, String code, String message) {
        return error(responseCode, code, message, Map.of("code", code, "message", message));
    }

    private static OpenApiError error(String responseCode, String code, String message, Object example) {
        return new OpenApiError(responseCode, code, message, example);
    }
}
