package withoutc.chongchong.notice.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.user.entity.User;

class NoticeAttachmentPolicyTest {

    private NoticeAttachmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new NoticeAttachmentPolicy(10, 5242880, List.of("image/jpeg", "image/png", "image/webp"));
    }

    @Test
    @DisplayName("최대 개수인 10개의 첨부 파일 ID를 허용한다")
    void acceptMaximumAttachmentCount() {
        List<Long> fileIds = LongStream.rangeClosed(1, 10).boxed().toList();

        assertThatCode(() -> policy.validateIds(fileIds)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("최대 개수를 한 개 초과한 첨부 목록을 거부한다")
    void rejectOverMaximumAttachmentCount() {
        List<Long> fileIds = LongStream.rangeClosed(1, 11).boxed().toList();

        assertThatThrownBy(() -> policy.validateIds(fileIds))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("JPEG·PNG·WebP 이미지를 허용한다")
    void acceptSupportedImageType(String contentType) {
        FileMetadata file = createFile(contentType, 1024);

        assertThatCode(() -> policy.validateImage(file)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "image/gif", "image/jpg"})
    @DisplayName("허용 목록에 없는 Content-Type은 거부한다")
    void rejectUnsupportedImageType(String contentType) {
        FileMetadata file = createFile(contentType, 1024);

        assertThatThrownBy(() -> policy.validateImage(file))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_TYPE);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 5242880L})
    @DisplayName("1바이트부터 정확히 5MiB까지의 이미지를 허용한다")
    void acceptImageSizeAtBoundaries(long size) {
        FileMetadata file = createFile("image/png", size);

        assertThatCode(() -> policy.validateImage(file)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5242881L})
    @DisplayName("0바이트와 5MiB를 한 바이트 초과한 이미지를 거부한다")
    void rejectImageSizeOutsideBoundaries(long size) {
        FileMetadata file = createFile("image/png", size);

        assertThatThrownBy(() -> policy.validateImage(file))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_SIZE);
    }

    @Test
    @DisplayName("Spring 설정으로 허용 개수·용량·형식을 변경할 수 있다")
    void applyConfiguredPolicy() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getBeanFactory()
                        .setConversionService(ApplicationConversionService.getSharedInstance()))
                .withUserConfiguration(NoticeAttachmentPolicy.class)
                .withPropertyValues(
                        "notice.attachments.max-count=2",
                        "notice.attachments.max-size-bytes=16",
                        "notice.attachments.allowed-content-types=image/gif,image/webp"
                )
                .run(context -> {
                    NoticeAttachmentPolicy configured = context.getBean(NoticeAttachmentPolicy.class);

                    assertThatCode(() -> configured.validateIds(List.of(1L, 2L))).doesNotThrowAnyException();
                    assertThatThrownBy(() -> configured.validateIds(List.of(1L, 2L, 3L)))
                            .isInstanceOf(NoticeException.class)
                            .extracting(exception -> ((NoticeException) exception).getErrorCode())
                            .isEqualTo(NoticeErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
                    assertThatCode(() -> configured.validateImage(createFile("image/gif", 16)))
                            .doesNotThrowAnyException();
                    assertThatCode(() -> configured.validateImage(createFile("image/webp", 16)))
                            .doesNotThrowAnyException();
                    assertThatThrownBy(() -> configured.validateImage(createFile("image/gif", 17)))
                            .isInstanceOf(NoticeException.class)
                            .extracting(exception -> ((NoticeException) exception).getErrorCode())
                            .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_SIZE);
                    assertThatThrownBy(() -> configured.validateImage(createFile("image/png", 16)))
                            .isInstanceOf(NoticeException.class)
                            .extracting(exception -> ((NoticeException) exception).getErrorCode())
                            .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_TYPE);
                });
    }

    private FileMetadata createFile(String contentType, long size) {
        return FileMetadata.create(User.create("사용자", null), "image.png", "uploads/image", contentType, size);
    }
}
