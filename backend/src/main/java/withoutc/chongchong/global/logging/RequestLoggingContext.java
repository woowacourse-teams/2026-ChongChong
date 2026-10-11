package withoutc.chongchong.global.logging;

import jakarta.servlet.http.HttpServletRequest;
import withoutc.chongchong.global.exception.code.ErrorCode;

public final class RequestLoggingContext {

    public static final String REQUEST_ID_ATTRIBUTE = "observability.request_id";

    public static final String START_TIME_ATTRIBUTE = "observability.start_time";

    public static final String ERROR_CODE_ATTRIBUTE = "observability.error_code";

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static final String MDC_REQUEST_ID_KEY = "request_id";

    private RequestLoggingContext() {
    }

    public static void recordErrorCode(HttpServletRequest request, ErrorCode errorCode) {
        request.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode.getCode());
    }
}
