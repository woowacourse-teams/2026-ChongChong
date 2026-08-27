package withoutc.chongchong.global.config.openapi;

import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.AUTHENTICATION_REQUIRED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_INPUT_VALUE;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST_PARAMETER;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.NOT_STUDY_LEADER;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.STUDY_ACCESS_DENIED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.STUDY_NOT_FOUND;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.USER_NOT_FOUND;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.entry;
import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.errors;

@Component
class StudyOpenApiErrorProvider extends AbstractOpenApiErrorProvider {

    private static final OpenApiError INVALID_INVITE_TOKEN = error(
            "400", "INVALID_INVITE_TOKEN", "유효하지 않은 초대 토큰입니다.",
            Map.of("code", "INVALID_INVITE_TOKEN", "message", "유효하지 않은 초대 토큰입니다.")
    );
    private static final OpenApiError JOINED_STUDY_LIMIT_EXCEEDED = error(
            "409", "JOINED_STUDY_LIMIT_EXCEEDED", "가입할 수 있는 스터디는 최대 50개입니다.",
            Map.of("code", "JOINED_STUDY_LIMIT_EXCEEDED", "message", "가입할 수 있는 스터디는 최대 50개입니다.")
    );
    private static final OpenApiError ALREADY_JOINED_STUDY = error(
            "409", "ALREADY_JOINED_STUDY", "해당 스터디에 이미 가입되어 있습니다.",
            Map.of("code", "ALREADY_JOINED_STUDY", "message", "해당 스터디에 이미 가입되어 있습니다.")
    );
    private static final OpenApiError STUDY_MEMBER_LIMIT_EXCEEDED = error(
            "409", "STUDY_MEMBER_LIMIT_EXCEEDED", "스터디 정원이 가득 찼습니다.",
            Map.of("code", "STUDY_MEMBER_LIMIT_EXCEEDED", "message", "스터디 정원이 가득 찼습니다.")
    );

    StudyOpenApiErrorProvider() {
        super(Map.ofEntries(
                entry("createStudy", errors(INVALID_INPUT_VALUE, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        JOINED_STUDY_LIMIT_EXCEEDED, USER_NOT_FOUND)),
                entry("getStudyDetail", errors(INVALID_REQUEST_PARAMETER, AUTHENTICATION_REQUIRED,
                        INVALID_REQUEST, STUDY_ACCESS_DENIED, STUDY_NOT_FOUND)),
                entry("deleteStudy", errors(INVALID_REQUEST_PARAMETER, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, NOT_STUDY_LEADER, STUDY_NOT_FOUND)),
                entry("getStudyInfo", errors(INVALID_REQUEST_PARAMETER, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, STUDY_NOT_FOUND)),
                entry("getMyStudies", errors(AUTHENTICATION_REQUIRED)),
                entry("getInviteLink", errors(INVALID_REQUEST_PARAMETER, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, STUDY_NOT_FOUND)),
                entry("join", errors(INVALID_INPUT_VALUE, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        INVALID_INVITE_TOKEN, USER_NOT_FOUND, STUDY_NOT_FOUND, ALREADY_JOINED_STUDY,
                        STUDY_MEMBER_LIMIT_EXCEEDED))
        ));
    }
}
