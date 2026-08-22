package withoutc.chongchong.global.exception.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.FieldError;
import withoutc.chongchong.global.exception.code.ErrorCode;

@JsonPropertyOrder({"code", "message", "errors"})
public record ErrorResponse(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorDetail> errors
) {
    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorDetail> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.copyOf(errors));
    }

    public record FieldErrorDetail(
            String code,
            String field,
            String reason
    ) {

        private static final String REQUEST_VALIDATION_PREFIX = "REQUEST_VALIDATION_";

        public static FieldErrorDetail from(FieldError fieldError) {
            return new FieldErrorDetail(toRequestValidationCode(fieldError), fieldError.getField(),
                    fieldError.getDefaultMessage());
        }

        public static FieldErrorDetail of(String field, MessageSourceResolvable error) {
            return new FieldErrorDetail(
                    toRequestValidationCode(error),
                    field,
                    error.getDefaultMessage()
            );
        }

        private static String toRequestValidationCode(MessageSourceResolvable error) {
            String[] codes = error.getCodes();

            if (codes == null || codes.length == 0) {
                return REQUEST_VALIDATION_PREFIX + "UNKNOWN";
            }

            String code = codes[codes.length - 1];

            return REQUEST_VALIDATION_PREFIX + toUpperSnakeCase(code);
        }

        private static String toUpperSnakeCase(String code) {
            return code.replaceAll("([A-Z])(?=[A-Z])", "$1_")
                    .replaceAll("([a-z])([A-Z])", "$1_$2")
                    .toUpperCase();
        }
    }
}
