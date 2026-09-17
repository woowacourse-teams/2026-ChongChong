package withoutc.chongchong.support.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class OpenApiOperationCatalogTest {

    @TempDir
    Path directory;

    @Test
    void 외부_경로와_응답_참조에서_API와_빈_본문_허용을_읽는다() throws IOException {
        Path spec = directory.resolve("openapi.yaml");
        Files.writeString(spec, """
                openapi: 3.1.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /api/items:
                    $ref: './items.yaml'
                """);
        Files.writeString(directory.resolve("items.yaml"), """
                get:
                  responses:
                    '200':
                      $ref: './responses.yaml#/Optional'
                """);
        Files.writeString(directory.resolve("responses.yaml"), """
                Optional:
                  description: optional result
                  x-allow-empty-body: true
                """);

        OpenApiOperationCatalog catalog = OpenApiOperationCatalog.load(spec);

        assertThat(catalog.operations()).containsExactly(new ApiOperation("GET", "/api/items"));
        assertThat(catalog.allowsEmptyBody("GET", "/api/items", 200)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"todo", "in-progress", "review", "done", "null"})
    void 대기와_진행중만_구현과_성공_호출을_유예하고_호출_대상에는_유지한다(String status) throws IOException {
        Path spec = directory.resolve("openapi.yaml");
        String metadata = status == null ? "" : "    x-backend:\n      status: " + status + "\n";
        Files.writeString(spec, """
                openapi: 3.1.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /api/items:
                    get:
                      responses:
                        '200':
                          description: success
                """ + metadata.indent(2));

        OpenApiOperationCatalog catalog = OpenApiOperationCatalog.load(spec);
        ApiOperation operation = new ApiOperation("GET", "/api/items");

        assertThat(catalog.operations()).containsExactly(operation);
        assertThat(catalog.resolve("GET", "/api/items")).isEqualTo(operation);
        if ("todo".equals(status) || "in-progress".equals(status)) {
            assertThat(catalog.requiredOperations()).isEmpty();
        } else {
            assertThat(catalog.requiredOperations()).containsExactly(operation);
        }
    }
}
