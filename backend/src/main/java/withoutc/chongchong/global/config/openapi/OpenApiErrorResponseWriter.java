package withoutc.chongchong.global.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.stereotype.Component;

@Component
class OpenApiErrorResponseWriter {

    private static final String ERROR_SCHEMA_NAME = "ErrorResponse";
    private static final String JSON_MEDIA_TYPE = "application/json";

    void addErrorSchema(Components components) {
        if (components.getSchemas() != null && components.getSchemas().containsKey(ERROR_SCHEMA_NAME)) {
            return;
        }

        Schema<?> fieldErrorSchema = new ObjectSchema()
                .addProperty("code", new StringSchema().description("필드 오류 코드").example("REQUEST_VALIDATION_NOT_BLANK"))
                .addProperty("field", new StringSchema().description("오류가 발생한 필드").example("title"))
                .addProperty("reason", new StringSchema().description("오류 사유").example("제목은 필수 값입니다."));

        components.addSchemas(
                ERROR_SCHEMA_NAME,
                new ObjectSchema()
                        .description("공통 오류 응답")
                        .addProperty("code", new StringSchema().description("오류 코드").example("INVALID_INPUT_VALUE"))
                        .addProperty("message", new StringSchema().description("오류 메시지").example("입력값이 올바르지 않습니다."))
                        .addProperty("errors", new ArraySchema().description("필드별 오류 목록").items(fieldErrorSchema))
        );
    }

    void addErrorResponse(Operation operation, OpenApiError error) {
        ApiResponse response = operation.getResponses().get(error.responseCode());
        if (response == null) {
            response = new ApiResponse().description(responseDescription(error.responseCode()));
            operation.getResponses().addApiResponse(error.responseCode(), response);
        }

        Content content = response.getContent();
        if (content == null) {
            content = new Content();
            response.setContent(content);
        }

        MediaType mediaType = content.get(JSON_MEDIA_TYPE);
        if (mediaType == null) {
            mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA_NAME));
            content.addMediaType(JSON_MEDIA_TYPE, mediaType);
        }

        if (mediaType.getExamples() == null || !mediaType.getExamples().containsKey(error.exampleKey())) {
            mediaType.addExamples(
                    error.exampleKey(),
                    new Example()
                            .summary(error.message())
                            .value(error.example())
            );
        }
    }

    private String responseDescription(String responseCode) {
        return switch (responseCode) {
            case "400" -> "잘못된 요청";
            case "401" -> "인증 실패";
            case "403" -> "접근 권한 없음";
            case "404" -> "리소스 없음";
            case "409" -> "요청 충돌";
            case "500" -> "서버 내부 오류";
            default -> "오류 응답";
        };
    }
}
