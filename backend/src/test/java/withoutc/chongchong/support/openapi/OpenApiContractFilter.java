package withoutc.chongchong.support.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.restassured.RestAssuredRequest;
import com.atlassian.oai.validator.restassured.RestAssuredResponse;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
final class OpenApiContractFilter implements Filter {

    private static final String TEST_API_PREFIX = "/api/test/";
    private static final String TEST_API_WITHOUT_GLOBAL_PREFIX = "/test/";
    private static final Object COVERAGE_FILE_LOCK = new Object();

    private final OpenApiInteractionValidator validator;
    private final OpenApiOperationCatalog catalog;
    private final Path coveragePath;

    OpenApiContractFilter() {
        Path specPath = Path.of(System.getProperty("openapi.spec.path"));
        this.validator = OpenApiInteractionValidator.createFor(specPath.toString()).build();
        this.catalog = OpenApiOperationCatalog.load(specPath);
        this.coveragePath = Path.of(System.getProperty("openapi.coverage.path"));
        installAsOnlyContractFilter();
    }

    OpenApiContractFilter(
            OpenApiInteractionValidator validator,
            OpenApiOperationCatalog catalog,
            Path coveragePath
    ) {
        this.validator = validator;
        this.catalog = catalog;
        this.coveragePath = coveragePath;
    }

    private void installAsOnlyContractFilter() {
        synchronized (RestAssured.class) {
            List<Filter> filters = new ArrayList<>(RestAssured.filters());
            filters.removeIf(OpenApiContractFilter.class::isInstance);
            filters.add(this);
            RestAssured.replaceFiltersWith(filters);
        }
    }

    @Override
    public Response filter(
            FilterableRequestSpecification requestSpec,
            FilterableResponseSpecification responseSpec,
            FilterContext context
    ) {
        Response response = context.next(requestSpec, responseSpec);
        String path = URI.create(requestSpec.getURI()).getPath();
        if (path.startsWith(TEST_API_PREFIX) || path.startsWith(TEST_API_WITHOUT_GLOBAL_PREFIX)) {
            return response;
        }

        String method = requestSpec.getMethod().toUpperCase();
        exposeCookiesAsHeader(requestSpec);
        com.atlassian.oai.validator.model.Response validatorResponse = RestAssuredResponse.of(response);
        ValidationReport report = response.getStatusCode() < 400
                ? validator.validateRequest(RestAssuredRequest.of(requestSpec)).merge(validator.validateResponse(
                        path,
                        Request.Method.valueOf(method),
                        validatorResponse
                ))
                : validator.validateResponse(
                        path,
                        Request.Method.valueOf(method),
                        validatorResponse
                );
        if (response.getBody().asByteArray().length == 0
                && catalog.allowsEmptyBody(method, path, response.getStatusCode())) {
            report = ValidationReport.from(report.getMessages().stream()
                    .filter(message -> !message.getKey().equals("validation.response.body.missing"))
                    .toList());
        }
        if (report.hasErrors()) {
            throw new AssertionError(JsonValidationReportFormat.getInstance().apply(report));
        }
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            recordSuccessfulCall(method, path);
        }
        return response;
    }

    private void exposeCookiesAsHeader(FilterableRequestSpecification requestSpec) {
        Cookies cookies = requestSpec.getCookies();
        if (cookies == null || !cookies.exist() || requestSpec.getHeaders().hasHeaderWithName("Cookie")) {
            return;
        }
        String cookieHeader = cookies.asList().stream()
                .map(Cookie::getName)
                .map(name -> name + "=" + cookies.getValue(name))
                .collect(Collectors.joining("; "));
        requestSpec.header("Cookie", cookieHeader);
    }

    private void recordSuccessfulCall(String method, String path) {
        synchronized (COVERAGE_FILE_LOCK) {
            try {
                Files.createDirectories(coveragePath.getParent());
                Files.writeString(
                        coveragePath,
                        method + " " + path + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException exception) {
                throw new IllegalStateException("OpenAPI 성공 호출 기록에 실패했습니다: " + coveragePath, exception);
            }
        }
    }
}
