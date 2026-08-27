package withoutc.chongchong.global.config.openapi;

import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.entry;
import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.errors;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.AUTHENTICATION_REQUIRED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST_PARAMETER;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.NOT_STUDY_LEADER;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.STUDY_ACCESS_DENIED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.STUDY_NOT_FOUND;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class StudyMemberOpenApiErrorProvider extends AbstractOpenApiErrorProvider {

    private static final OpenApiError INVALID_MEMBER_ID_PARAMETER = error(
            "400", "INVALID_REQUEST_PARAMETER", "요청 파라미터가 올바르지 않습니다.",
            Map.of(
                    "code", "INVALID_REQUEST_PARAMETER",
                    "message", "요청 파라미터가 올바르지 않습니다.",
                    "errors", List.of(Map.of(
                            "code", "REQUEST_VALIDATION_POSITIVE",
                            "field", "memberId",
                            "reason", "스터디 멤버 ID는 양수여야 합니다."
                    ))
            )
    );
    private static final OpenApiError STUDY_MEMBER_NOT_FOUND = simple(
            "404", "STUDY_MEMBER_NOT_FOUND", "존재하지 않는 스터디 멤버입니다."
    );
    private static final OpenApiError STUDY_LEADER_CANNOT_BE_REMOVED = simple(
            "403", "STUDY_LEADER_CANNOT_BE_REMOVED", "스터디 리더는 방출할 수 없습니다."
    );
    private static final OpenApiError STUDY_LEADER_CANNOT_LEAVE = simple(
            "403", "STUDY_LEADER_CANNOT_LEAVE", "스터디 리더는 탈퇴할 수 없습니다."
    );

    StudyMemberOpenApiErrorProvider() {
        super(Map.ofEntries(
                entry("getAllStudyMembers", errors(INVALID_REQUEST_PARAMETER, INVALID_REQUEST,
                        AUTHENTICATION_REQUIRED, STUDY_ACCESS_DENIED, STUDY_NOT_FOUND)),
                entry("expel", errors(INVALID_MEMBER_ID_PARAMETER, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, NOT_STUDY_LEADER, STUDY_MEMBER_NOT_FOUND,
                        STUDY_LEADER_CANNOT_BE_REMOVED)),
                entry("leave", errors(INVALID_REQUEST_PARAMETER, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, STUDY_LEADER_CANNOT_LEAVE))
        ));
    }

    private static OpenApiError simple(String responseCode, String code, String message) {
        return error(responseCode, code, message, Map.of("code", code, "message", message));
    }
}
