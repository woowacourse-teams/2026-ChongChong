package withoutc.chongchong.support.openapi;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.FilterContext;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiContractFilterTest {

    private static final String SPEC = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /api/items:
                get:
                  parameters:
                    - in: query
                      name: requiredValue
                      required: true
                      schema:
                        type: string
                  responses:
                    '200':
                      description: success
                      content:
                        application/json:
                          schema:
                            type: object
                            required: [id]
                            properties:
                              id:
                                type: integer
                    '400':
                      description: invalid request
                      content:
                        application/json:
                          schema:
                            type: object
                            required: [code]
                            properties:
                              code:
                                type: string
              /api/results:
                get:
                  responses:
                    '200':
                      description: success
                      content:
                        application/json:
                          schema:
                            type: object
                            required: [id]
                            properties:
                              id:
                                type: integer
              /api/optional-result:
                get:
                  responses:
                    '200':
                      description: optional result
                      x-allow-empty-body: true
                      content:
                        application/json:
                          schema:
                            type: object
              /api/unexpected-empty-result:
                get:
                  responses:
                    '200':
                      description: required result
                      content:
                        application/json:
                          schema:
                            type: object
            """;

    @TempDir
    Path tempDirectory;

    @Test
    void 잘못된_요청도_응답만_명세와_비교한다() {
        OpenApiContractFilter filter = createFilter();
        FilterableRequestSpecification request = request("GET", "http://localhost/api/items");
        Response response = response(400, "{\"code\":\"INVALID_REQUEST\"}");
        FilterContext context = contextReturning(response);

        assertThatCode(() -> filter.filter(request, mock(FilterableResponseSpecification.class), context))
                .doesNotThrowAnyException();
    }

    @Test
    void 응답_스키마가_명세와_다르면_실패한다() {
        OpenApiContractFilter filter = createFilter();
        FilterableRequestSpecification request = request("GET", "http://localhost/api/results");
        Response response = response(200, "{\"id\":\"문자열\"}");
        FilterContext context = contextReturning(response);

        assertThatThrownBy(() -> filter.filter(request, mock(FilterableResponseSpecification.class), context))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("id");
    }

    @Test
    void 확장으로_명시한_200_응답만_빈_본문을_허용한다() {
        OpenApiContractFilter filter = createFilter();
        FilterableRequestSpecification request = request("GET", "http://localhost/api/optional-result");
        Response response = response(200, "");
        FilterContext context = contextReturning(response);

        assertThatCode(() -> filter.filter(request, mock(FilterableResponseSpecification.class), context))
                .doesNotThrowAnyException();
    }

    @Test
    void 확장으로_명시하지_않은_빈_본문은_실패한다() {
        OpenApiContractFilter filter = createFilter();
        FilterableRequestSpecification request = request("GET", "http://localhost/api/unexpected-empty-result");
        Response response = response(200, "");
        FilterContext context = contextReturning(response);

        assertThatThrownBy(() -> filter.filter(request, mock(FilterableResponseSpecification.class), context))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("validation.response.body.missing");
    }

    @Test
    void 오류_응답_스키마가_명세와_다르면_실패한다() {
        OpenApiContractFilter filter = createFilter();
        FilterableRequestSpecification request = request("GET", "http://localhost/api/items");
        Response response = response(400, "{\"message\":\"필수 code 없음\"}");
        FilterContext context = contextReturning(response);

        assertThatThrownBy(() -> filter.filter(request, mock(FilterableResponseSpecification.class), context))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("code");
    }

    @Test
    void 명세에_선언하지_않은_응답_상태는_실패한다() {
        OpenApiContractFilter filter = createFilter();
        FilterableRequestSpecification request = request("GET", "http://localhost/api/items");
        Response response = response(418, "{\"code\":\"TEAPOT\"}");
        FilterContext context = contextReturning(response);

        assertThatThrownBy(() -> filter.filter(request, mock(FilterableResponseSpecification.class), context))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("validation.response.status.unknown");
    }

    private OpenApiContractFilter createFilter() {
        Path specPath = tempDirectory.resolve("openapi.yaml");
        try {
            java.nio.file.Files.writeString(specPath, SPEC);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath.toString()).build();
        return new OpenApiContractFilter(
                validator,
                OpenApiOperationCatalog.load(specPath),
                tempDirectory.resolve("coverage.txt")
        );
    }

    private FilterableRequestSpecification request(String method, String uri) {
        FilterableRequestSpecification request = mock(FilterableRequestSpecification.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getURI()).thenReturn(uri);
        when(request.getDerivedPath()).thenReturn(URI.create(uri).getPath());
        when(request.getHeaders()).thenReturn(new Headers());
        when(request.getQueryParams()).thenReturn(Map.of());
        when(request.getRequestParams()).thenReturn(Map.of());
        return request;
    }

    private Response response(int status, String body) {
        return new ResponseBuilder()
                .setStatusCode(status)
                .setContentType("application/json")
                .setBody(body)
                .build();
    }

    private FilterContext contextReturning(Response response) {
        FilterContext context = mock(FilterContext.class);
        when(context.next(
                org.mockito.ArgumentMatchers.any(FilterableRequestSpecification.class),
                org.mockito.ArgumentMatchers.any(FilterableResponseSpecification.class)
        )).thenReturn(response);
        return context;
    }
}
