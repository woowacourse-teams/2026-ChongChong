package withoutc.chongchong.file.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "file.cleanup")
public record FileCleanupProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("true") boolean dryRun,
        @DefaultValue("100") int batchSize,
        @DefaultValue("20") int maxBatchesPerRun,
        @DefaultValue("1h") Duration pendingGrace,
        @DefaultValue("24h") Duration unreferencedGrace,
        @DefaultValue("10m") Duration deleteLease,
        @DefaultValue("5") int maxAttempts
) {

    static final Duration CLOCK_SKEW_MARGIN = Duration.ofMinutes(5);

    public FileCleanupProperties {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("파일 정리 배치 크기는 1 이상 1000 이하여야 합니다.");
        }
        if (maxBatchesPerRun < 1) {
            throw new IllegalArgumentException("파일 정리 실행당 배치 수는 1 이상이어야 합니다.");
        }
        requirePositive(pendingGrace, "업로드 미완료 파일 유예 시간");
        requirePositive(unreferencedGrace, "미사용 파일 유예 시간");
        requirePositive(deleteLease, "삭제 선점 유지 시간");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("파일 삭제 최대 시도 횟수는 1 이상이어야 합니다.");
        }
    }

    public void validateAgainstUploadUrlValidity(Duration uploadUrlValidity) {
        Duration minimumGrace = uploadUrlValidity.plus(CLOCK_SKEW_MARGIN);
        if (pendingGrace.compareTo(minimumGrace) <= 0 || unreferencedGrace.compareTo(minimumGrace) <= 0) {
            throw new IllegalArgumentException(
                    "파일 정리 유예 시간은 업로드 URL 유효기간에 5분을 더한 값보다 길어야 합니다.");
        }
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(name + "은 0보다 커야 합니다.");
        }
    }
}
