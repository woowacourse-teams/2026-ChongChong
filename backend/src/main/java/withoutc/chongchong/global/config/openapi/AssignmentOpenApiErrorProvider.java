package withoutc.chongchong.global.config.openapi;

import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.ACCESS_DENIED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.AUTHENTICATION_REQUIRED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_INPUT_VALUE;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST_PARAMETER;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.STUDY_ACCESS_DENIED;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class AssignmentOpenApiErrorProvider extends AbstractOpenApiErrorProvider {

    private static final OpenApiError ASSIGNMENT_NOT_FOUND = simple(
            "404", "ASSIGNMENT_NOT_FOUND", "존재하지 않는 과제입니다."
    );
    private static final OpenApiError ASSIGNMENT_SUBMISSION_NOT_FOUND = simple(
            "404", "ASSIGNMENT_SUBMISSION_NOT_FOUND", "존재하지 않는 제출물입니다."
    );
    private static final OpenApiError INVALID_TITLE = simple(
            "400", "INVALID_TITLE", "과제 제목이 올바르지 않습니다."
    );
    private static final OpenApiError INVALID_CONTENT = simple(
            "400", "INVALID_CONTENT", "과제 내용이 올바르지 않습니다."
    );
    private static final OpenApiError INVALID_LINK = simple(
            "400", "INVALID_LINK", "과제 링크가 올바르지 않습니다."
    );
    private static final OpenApiError INVALID_SUBMISSION_METHOD = simple(
            "400", "INVALID_SUBMISSION_METHOD", "제출 방법이 올바르지 않습니다."
    );
    private static final OpenApiError INVALID_CLOSE_AT = simple(
            "400", "INVALID_CLOSE_AT", "과제 마감 시각이 올바르지 않습니다."
    );
    private static final OpenApiError INVALID_REMIND_AT = simple(
            "400", "INVALID_REMIND_AT", "리마인드 시각이 올바르지 않습니다."
    );

    AssignmentOpenApiErrorProvider() {
        super(Map.ofEntries(
                entry("createAssignment", errors(INVALID_INPUT_VALUE, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ACCESS_DENIED, INVALID_TITLE, INVALID_CONTENT, INVALID_SUBMISSION_METHOD,
                        INVALID_CLOSE_AT, INVALID_REMIND_AT)),
                entry("getAssignmentDetail", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ASSIGNMENT_NOT_FOUND)),
                entry("getAssignments", errors(INVALID_REQUEST_PARAMETER, INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ASSIGNMENT_SUBMISSION_NOT_FOUND)),
                entry("updateAssignment", errors(INVALID_INPUT_VALUE, INVALID_REQUEST,
                        AUTHENTICATION_REQUIRED, STUDY_ACCESS_DENIED, ACCESS_DENIED, ASSIGNMENT_NOT_FOUND,
                        INVALID_TITLE, INVALID_CONTENT, INVALID_SUBMISSION_METHOD, INVALID_CLOSE_AT,
                        INVALID_REMIND_AT)),
                entry("deleteAssignment", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ACCESS_DENIED, ASSIGNMENT_NOT_FOUND)),
                entry("submitAssignment", errors(INVALID_INPUT_VALUE, INVALID_REQUEST,
                        AUTHENTICATION_REQUIRED, STUDY_ACCESS_DENIED, ASSIGNMENT_NOT_FOUND,
                        ASSIGNMENT_SUBMISSION_NOT_FOUND, INVALID_CONTENT, INVALID_LINK)),
                entry("updateSubmission", errors(INVALID_INPUT_VALUE, INVALID_REQUEST,
                        AUTHENTICATION_REQUIRED, STUDY_ACCESS_DENIED, ASSIGNMENT_NOT_FOUND,
                        ASSIGNMENT_SUBMISSION_NOT_FOUND, INVALID_CONTENT, INVALID_LINK)),
                entry("getAssignmentSubmissionStatus", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ACCESS_DENIED, ASSIGNMENT_NOT_FOUND)),
                entry("getMySubmissionDetail", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ASSIGNMENT_NOT_FOUND)),
                entry("getSubmissionDetail", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ASSIGNMENT_NOT_FOUND, ASSIGNMENT_SUBMISSION_NOT_FOUND)),
                entry("getSubmissionList", errors(INVALID_REQUEST, AUTHENTICATION_REQUIRED,
                        STUDY_ACCESS_DENIED, ACCESS_DENIED, ASSIGNMENT_NOT_FOUND))
        ));
    }

    private static OpenApiError simple(String responseCode, String code, String message) {
        return error(responseCode, code, message, Map.of("code", code, "message", message));
    }
}
