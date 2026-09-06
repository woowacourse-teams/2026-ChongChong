package withoutc.chongchong.support.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiMappingContractTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void 명세와_업무_API의_HTTP_메서드와_경로가_양방향으로_일치한다() {
        OpenApiOperationCatalog catalog = OpenApiOperationCatalog.load(
                Path.of(System.getProperty("openapi.spec.path"))
        );

        Set<ApiOperation> implemented = new TreeSet<>();
        Set<String> mappingsWithoutHttpMethod = new TreeSet<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) -> {
            if (!handlerMethod.getBeanType().getPackageName().startsWith("withoutc.chongchong")) {
                return;
            }
            mapping.getPatternValues().stream()
                    .filter(path -> !path.startsWith("/api/test/"))
                    .forEach(path -> {
                        Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
                        if (methods.isEmpty()) {
                            mappingsWithoutHttpMethod.add(path);
                            return;
                        }
                        methods.stream()
                                .map(RequestMethod::name)
                                .map(method -> new ApiOperation(method, path))
                                .forEach(implemented::add);
                    });
        });

        assertThat(mappingsWithoutHttpMethod)
                .as("HTTP 메서드를 명시하지 않은 업무 API")
                .isEmpty();
        assertThat(implemented).containsExactlyInAnyOrderElementsOf(catalog.operations());
    }
}
