package withoutc.chongchong.global.config.openapi;

import static withoutc.chongchong.global.config.openapi.AbstractOpenApiErrorProvider.errors;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.AUTHENTICATION_REQUIRED;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_INPUT_VALUE;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.INVALID_REQUEST;
import static withoutc.chongchong.global.config.openapi.OpenApiCommonErrors.USER_NOT_FOUND;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class NotificationOpenApiErrorProvider extends AbstractOpenApiErrorProvider {

    NotificationOpenApiErrorProvider() {
        super(Map.of(
                "createPushToken",
                errors(INVALID_INPUT_VALUE, INVALID_REQUEST, AUTHENTICATION_REQUIRED, USER_NOT_FOUND)
        ));
    }
}
