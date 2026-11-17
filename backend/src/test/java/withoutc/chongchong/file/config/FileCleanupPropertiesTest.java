package withoutc.chongchong.file.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileCleanupPropertiesTest {

    @Test
    @DisplayName("유예 시간이 업로드 URL 유효기간과 시계 오차 여유를 합한 값보다 짧으면 시작하지 않는다")
    void rejectGraceShorterThanUploadUrlValidity() {
        FileCleanupProperties properties = properties(Duration.ofMinutes(15), Duration.ofHours(24));

        assertThatThrownBy(() -> properties.validateAgainstUploadUrlValidity(Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("미사용 파일 유예 시간도 업로드 URL 유효기간보다 길어야 한다")
    void rejectUnreferencedGraceShorterThanUploadUrlValidity() {
        FileCleanupProperties properties = properties(Duration.ofHours(1), Duration.ofMinutes(10));

        assertThatThrownBy(() -> properties.validateAgainstUploadUrlValidity(Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기본 유예 시간은 기본 업로드 URL 유효기간에서 통과한다")
    void acceptDefaultGrace() {
        FileCleanupProperties properties = properties(Duration.ofHours(1), Duration.ofHours(24));

        assertThatCode(() -> properties.validateAgainstUploadUrlValidity(Duration.ofMinutes(10)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("배치 크기는 S3 일괄 삭제 한도인 1000을 넘을 수 없다")
    void rejectBatchSizeOverS3Limit() {
        assertThatThrownBy(() -> new FileCleanupProperties(
                false, true, 1001, 1, Duration.ofHours(1), Duration.ofHours(24), Duration.ofMinutes(10), 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private FileCleanupProperties properties(Duration pendingGrace, Duration unreferencedGrace) {
        return new FileCleanupProperties(
                false, true, 100, 20, pendingGrace, unreferencedGrace, Duration.ofMinutes(10), 5);
    }
}
