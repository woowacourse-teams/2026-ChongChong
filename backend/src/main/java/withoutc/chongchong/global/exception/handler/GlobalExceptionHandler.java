package withoutc.chongchong.global.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import withoutc.chongchong.global.exception.BusinessException;
import withoutc.chongchong.global.exception.code.CommonErrorCode;
import withoutc.chongchong.global.exception.code.ErrorCode;
import withoutc.chongchong.global.exception.response.ErrorResponse;
import withoutc.chongchong.global.exception.response.ErrorResponse.FieldErrorDetail;
import withoutc.chongchong.global.logging.RequestLoggingContext;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
        recordErrorCode(request, errorCode);

        List<FieldErrorDetail> errors = e.getFieldErrors()
                .stream()
                .map(error -> FieldErrorDetail.of(error.getField(), error))
                .toList();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode, errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidationException(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {
        if (e.isForReturnValue()) {
            return handleException(e, request);
        }

        ErrorCode errorCode = CommonErrorCode.INVALID_REQUEST_PARAMETER;
        recordErrorCode(request, errorCode);

        List<FieldErrorDetail> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> FieldErrorDetail.of(
                                result.getMethodParameter().getParameterName(),
                                error)
                        )
                )
                .toList();

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, errors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_REQUEST;
        recordErrorCode(request, errorCode);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMethodException(HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.UNSUPPORTED_HTTP_METHOD;
        recordErrorCode(request, errorCode);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleUnsupportedPathException(HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.UNSUPPORTED_PATH;
        recordErrorCode(request, errorCode);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        recordErrorCode(request, errorCode);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        recordErrorCode(request, errorCode);
        log.error("[처리하지 못한 예외가 발생했습니다] : {}", e.getMessage(), e);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
    }

    private void recordErrorCode(HttpServletRequest request, ErrorCode errorCode) {
        RequestLoggingContext.recordErrorCode(request, errorCode);
    }
}
