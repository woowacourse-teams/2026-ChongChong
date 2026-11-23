package withoutc.chongchong.assignment.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "assignment.recurrence")
public record AssignmentRecurrenceProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("1m") Duration fixedDelay,
        @DefaultValue("100") int batchSize,
        @DefaultValue("10m") Duration retryDelay
) {

    // 잘못된 값이면 앱이 뜨지 않게 해서 배포 단계에서 바로 드러나게 한다.
    public AssignmentRecurrenceProperties {
        requireAtLeastOneSecond(fixedDelay, "반복 과제 워커 실행 주기");
        requireAtLeastOneSecond(retryDelay, "반복 과제 재시도 대기 시간");
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("반복 과제 워커 배치 크기는 1 이상 1000 이하여야 합니다.");
        }
    }

    private static void requireAtLeastOneSecond(Duration duration, String name) {
        if (duration == null || duration.compareTo(Duration.ofSeconds(1)) < 0) {
            throw new IllegalArgumentException(name + "은 1초 이상이어야 합니다.");
        }
    }
}
