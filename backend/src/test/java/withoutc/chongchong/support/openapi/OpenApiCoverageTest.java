package withoutc.chongchong.support.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("openapi-coverage")
class OpenApiCoverageTest {

    @Test
    void 모든_명세_API를_성공_응답으로_호출한다() throws IOException {
        OpenApiOperationCatalog catalog = OpenApiOperationCatalog.load(
                Path.of(System.getProperty("openapi.spec.path"))
        );
        Path coveragePath = Path.of(System.getProperty("openapi.coverage.path"));
        List<String> successfulCalls = Files.exists(coveragePath)
                ? Files.readAllLines(coveragePath)
                : List.of();

        Set<ApiOperation> covered = catalog.resolveAll(successfulCalls);

        assertThat(covered).containsExactlyInAnyOrderElementsOf(catalog.operations());
    }
}
