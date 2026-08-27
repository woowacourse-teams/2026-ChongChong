package withoutc.chongchong.global.config.openapi;

import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.entry;
import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.errors;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.ACCESS_DENIED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.AUTHENTICATION_REQUIRED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_INPUT_VALUE;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST_PARAMETER;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.STUDY_ACCESS_DENIED;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class NoticeOpenApiErrorProvider extends AbstractOpenApiErrorProvider {

    private static final OpenApiError NOTICE_NOT_FOUND = simple(
            "404", "NOTICE_NOT_FOUND", "존재하지 않는 공지입니다."
    );
    private static final OpenApiError NOTICE_RECIPIENT_NOT_FOUND = simple(
            "404", "NOTICE_RECIPIENT_NOT_FOUND", "공지 수신자 정보를 찾을 수 없습니다."
    );
    private static final OpenApiError INVALID_TITLE = simple(
            "400", "INVALID_TITLE", "공지 제목이 올바르지 않습니다."
    );
    private static final OpenApiError INVALID_CONTENT = simple(
            "400", "INVALID_CONTENT", "공지 내용이 올바르지 않습니다."
    );
    private static final OpenApiError INVALID_REMIND_AT = simple(
            "400", "INVALID_REMIND_AT", "리마인드 시각이 올바르지 않습니다."
    );

    NoticeOpenApiErrorProvider() {
        super(Map.ofEntries(
                entry("createNotice", errors(INVALID_INPUT_VALUE, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ACCESS_DENIED, INVALID_TITLE, INVALID_CONTENT, INVALID_REMIND_AT)),
                entry("getNoticeDetail", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, NOTICE_NOT_FOUND)),
                entry("getNotices", errors(INVALID_REQUEST_PARAMETER, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, NOTICE_RECIPIENT_NOT_FOUND)),
                entry("getAllReadStatuses", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ACCESS_DENIED, NOTICE_NOT_FOUND)),
                entry("updateNotice", errors(INVALID_INPUT_VALUE, INVALID_REQUEST,
                        AUTHENTICATION_REQUIRED, STUDY_ACCESS_DENIED, ACCESS_DENIED, NOTICE_NOT_FOUND,
                        INVALID_TITLE, INVALID_CONTENT, INVALID_REMIND_AT)),
                entry("deleteNotice", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ACCESS_DENIED, NOTICE_NOT_FOUND)),
                entry("readNotice", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, NOTICE_NOT_FOUND, NOTICE_RECIPIENT_NOT_FOUND)),
                entry("getMyReadStatus", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, NOTICE_NOT_FOUND, NOTICE_RECIPIENT_NOT_FOUND))
        ));
    }

    private static OpenApiError simple(String responseCode, String code, String message) {
        return error(responseCode, code, message, Map.of("code", code, "message", message));
    }
}
