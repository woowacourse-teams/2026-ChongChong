package withoutc.chongchong.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

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
            String field,
            String reason
    ) {
        public static FieldErrorDetail of(String field, String reason) {
            return new FieldErrorDetail(field, reason);
        }
    }
}
