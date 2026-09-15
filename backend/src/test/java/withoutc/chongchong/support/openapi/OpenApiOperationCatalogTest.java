package withoutc.chongchong.support.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
