package withoutc.chongchong.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.file.service.S3FileMetadataReader;

// API 테스트가 하나의 컨텍스트를 공유하도록 S3 대역 설정을 모은다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3PresignerConfiguration.class)
public abstract class PostgresApiTest extends PostgresContainerTest {

    @MockitoBean
    protected S3FileMetadataReader metadataReader;

    @MockitoSpyBean
    protected FileUrlGenerator urlGenerator;
}
