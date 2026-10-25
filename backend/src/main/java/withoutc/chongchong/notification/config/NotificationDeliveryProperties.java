package withoutc.chongchong.notification.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "web-push.delivery")
public record NotificationDeliveryProperties(
        @DefaultValue("1") int concurrency,
        @DefaultValue("5s") Duration sendTimeout,
        @DefaultValue("8s") Duration shutdownTimeout
) {

    public NotificationDeliveryProperties {
        if (concurrency < 1) {
            throw new IllegalArgumentException("푸시 알림 동시 발송 수는 1 이상이어야 합니다.");
        }
        if (sendTimeout == null || sendTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("푸시 발송 제한 시간은 1밀리초 이상이어야 합니다.");
        }
        if (shutdownTimeout == null || shutdownTimeout.compareTo(sendTimeout.plusSeconds(1)) < 0) {
            throw new IllegalArgumentException("종료 대기 시간은 발송 제한 시간보다 1초 이상 길어야 합니다.");
        }
    }
}
