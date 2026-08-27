package withoutc.chongchong.global.config.openapi;

import java.util.List;
import java.util.Map;

abstract class AbstractOpenApiErrorProvider implements OpenApiErrorProvider {

    private final Map<String, List<OpenApiError>> errorsByOperation;

    protected AbstractOpenApiErrorProvider(Map<String, List<OpenApiError>> errorsByOperation) {
        this.errorsByOperation = errorsByOperation;
    }

    @Override
    public boolean supports(String operationId) {
        return errorsByOperation.containsKey(operationId);
    }

    @Override
    public List<OpenApiError> errorsFor(String operationId) {
        return errorsByOperation.getOrDefault(operationId, List.of());
    }

    protected static OpenApiError error(String responseCode, String code, String message, Object example) {
        return new OpenApiError(responseCode, code, message, example);
    }

    protected static OpenApiError error(
            String responseCode,
            String code,
            String message,
            Object example,
            String exampleKey
    ) {
        return new OpenApiError(responseCode, code, message, example, exampleKey);
    }

    protected static List<OpenApiError> errors(OpenApiError... errors) {
        return List.of(errors);
    }

    protected static Map.Entry<String, List<OpenApiError>> entry(
            String operationId,
            List<OpenApiError> errors
    ) {
        return Map.entry(operationId, errors);
    }
}
