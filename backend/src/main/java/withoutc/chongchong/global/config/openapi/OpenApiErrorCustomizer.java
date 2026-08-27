package withoutc.chongchong.global.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenApiErrorCustomizer implements OpenApiCustomizer {

    private final OpenApiErrorResponseWriter responseWriter;
    private final List<OpenApiErrorProvider> errorProviders;

    @Override
    public void customise(OpenAPI openAPI) {
        addErrorSchema(openAPI);

        if (openAPI.getPaths() == null) {
            return;
        }

        openAPI.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .forEach(operation -> {
                    responseWriter.addErrorResponse(operation, OpenApiCommonErrors.INTERNAL_SERVER_ERROR);
                    errorProviders.stream()
                            .filter(provider -> provider.supports(operation.getOperationId()))
                            .findFirst()
                            .ifPresent(provider -> provider.errorsFor(operation.getOperationId())
                                    .forEach(error -> responseWriter.addErrorResponse(operation, error)));
                });
    }

    private void addErrorSchema(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            components = new Components();
            openAPI.setComponents(components);
        }
        responseWriter.addErrorSchema(components);
    }
}
