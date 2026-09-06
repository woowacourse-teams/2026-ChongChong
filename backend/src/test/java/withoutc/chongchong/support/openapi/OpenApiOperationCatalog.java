package withoutc.chongchong.support.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

final class OpenApiOperationCatalog {

    private final Set<ApiOperation> operations;
    private final List<PatternOperation> patternOperations;
    private final Set<EmptyBodyResponse> emptyBodyResponses;

    private OpenApiOperationCatalog(Set<ApiOperation> operations, Set<EmptyBodyResponse> emptyBodyResponses) {
        this.operations = Set.copyOf(operations);
        this.emptyBodyResponses = Set.copyOf(emptyBodyResponses);
        this.patternOperations = operations.stream()
                .map(operation -> new PatternOperation(
                        PathPatternParser.defaultInstance.parse(operation.path()),
                        operation
                ))
                .toList();
    }

    static OpenApiOperationCatalog load(Path specPath) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(specPath.toString(), null, options);
        OpenAPI openApi = result.getOpenAPI();
        if (openApi == null || result.getMessages() != null && !result.getMessages().isEmpty()) {
            throw new IllegalStateException("OpenAPI 명세를 읽을 수 없습니다: " + result.getMessages());
        }

        Set<ApiOperation> operations = new TreeSet<>();
        Set<EmptyBodyResponse> emptyBodyResponses = new TreeSet<>();
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
            ApiOperation apiOperation = new ApiOperation(method.name(), path);
            operations.add(apiOperation);
            operation.getResponses().forEach((status, response) -> {
                if (response.getExtensions() != null
                        && Boolean.TRUE.equals(response.getExtensions().get("x-allow-empty-body"))) {
                    emptyBodyResponses.add(new EmptyBodyResponse(apiOperation, Integer.parseInt(status)));
                }
            });
        }));
        return new OpenApiOperationCatalog(operations, emptyBodyResponses);
    }

    Set<ApiOperation> operations() {
        return operations;
    }

    ApiOperation resolve(String method, String concretePath) {
        PathContainer path = PathContainer.parsePath(concretePath);
        return patternOperations.stream()
                .filter(candidate -> candidate.operation().method().equals(method))
                .filter(candidate -> candidate.pattern().matches(path))
                .min(Comparator.comparing(PatternOperation::pattern, PathPattern.SPECIFICITY_COMPARATOR))
                .map(PatternOperation::operation)
                .orElseThrow(() -> new IllegalArgumentException("명세에 없는 API 호출입니다: " + method + " " + concretePath));
    }

    Set<ApiOperation> resolveAll(Collection<String> calls) {
        Set<ApiOperation> covered = new TreeSet<>();
        for (String call : calls) {
            int separator = call.indexOf(' ');
            if (separator < 1) {
                throw new IllegalArgumentException("잘못된 API 호출 기록입니다: " + call);
            }
            covered.add(resolve(call.substring(0, separator), call.substring(separator + 1)));
        }
        return covered;
    }

    boolean allowsEmptyBody(String method, String concretePath, int status) {
        return emptyBodyResponses.contains(new EmptyBodyResponse(resolve(method, concretePath), status));
    }

    private record PatternOperation(PathPattern pattern, ApiOperation operation) {
    }

    private record EmptyBodyResponse(ApiOperation operation, int status) implements Comparable<EmptyBodyResponse> {

        @Override
        public int compareTo(EmptyBodyResponse other) {
            int operationComparison = operation.compareTo(other.operation);
            if (operationComparison != 0) {
                return operationComparison;
            }
            return Integer.compare(status, other.status);
        }
    }
}
