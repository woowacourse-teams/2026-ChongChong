package withoutc.chongchong.global.config.openapi;

import java.util.List;

interface OpenApiErrorProvider {

    boolean supports(String operationId);

    List<OpenApiError> errorsFor(String operationId);
}
