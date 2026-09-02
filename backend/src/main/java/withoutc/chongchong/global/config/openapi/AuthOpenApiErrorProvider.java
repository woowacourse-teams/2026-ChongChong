package withoutc.chongchong.global.config.openapi;

import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.entry;
import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.errors;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_CSRF_TOKEN;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_INPUT_VALUE;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.USER_NOT_FOUND;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class AuthOpenApiErrorProvider extends AbstractOpenApiErrorProvider {

    private static final OpenApiError INVALID_REFRESH_TOKEN = simple(
            "401", "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."
    );
    private static final OpenApiError SOCIAL_AUTHENTICATION_FAILED = simple(
            "401", "SOCIAL_AUTHENTICATION_FAILED", "소셜 로그인 인증에 실패했습니다."
    );
    private static final OpenApiError UNSUPPORTED_SOCIAL_PROVIDER = simple(
            "400", "UNSUPPORTED_SOCIAL_PROVIDER", "지원하지 않는 소셜 로그인 제공자입니다."
    );

    AuthOpenApiErrorProvider() {
        super(Map.ofEntries(
                entry("csrf", errors()),
                entry("login", errors(INVALID_INPUT_VALUE, INVALID_REQUEST, INVALID_CSRF_TOKEN,
                        UNSUPPORTED_SOCIAL_PROVIDER, SOCIAL_AUTHENTICATION_FAILED, USER_NOT_FOUND)),
                entry("refresh", errors(INVALID_REQUEST, INVALID_CSRF_TOKEN, INVALID_REFRESH_TOKEN)),
                entry("logout", errors(INVALID_CSRF_TOKEN))
        ));
    }

    private static OpenApiError simple(String responseCode, String code, String message) {
        return error(responseCode, code, message, Map.of("code", code, "message", message));
    }
}
