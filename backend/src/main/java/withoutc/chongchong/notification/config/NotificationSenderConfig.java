package withoutc.chongchong.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tools.jackson.databind.ObjectMapper;
import withoutc.chongchong.notification.config.NotificationSenderProperties.Mode;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.sender.SimulatedNotificationSender;
import withoutc.chongchong.notification.sender.WebPushClient;
import withoutc.chongchong.notification.sender.WebPushNotificationSender;
import withoutc.chongchong.notification.token.WebPushNotificationClickTokenProvider;

@Slf4j
@Configuration
@EnableConfigurationProperties({NotificationSenderProperties.class, NotificationDeliveryProperties.class})
public class NotificationSenderConfig {

    private static final Profiles LOAD_TEST_PROFILE = Profiles.of("load-test");
    private static final Profiles PROD_PROFILE = Profiles.of("prod");

    private final NotificationSenderProperties properties;

    public NotificationSenderConfig(NotificationSenderProperties properties, Environment environment) {
        validateMode(properties.mode(), environment);
        this.properties = properties;
    }

    private void validateMode(Mode mode, Environment environment) {
        if (mode == Mode.SIMULATED) {
            validateSimulatedMode(environment);
            return;
        }
        validateRealMode(environment);
    }

    private void validateSimulatedMode(Environment environment) {
        boolean loadTestProfileActive = environment.acceptsProfiles(LOAD_TEST_PROFILE);
        boolean prodProfileActive = environment.acceptsProfiles(PROD_PROFILE);
        boolean devEnvironment = "dev".equals(environment.getProperty("app.environment"));

        if (!loadTestProfileActive || prodProfileActive || !devEnvironment) {
            throw new IllegalStateException(
                    "알림 시뮬레이터는 prod 프로필 없이 load-test 프로필과 app.environment=dev에서만 사용할 수 있습니다.");
        }
    }

    private void validateRealMode(Environment environment) {
        if (environment.acceptsProfiles(LOAD_TEST_PROFILE)) {
            throw new IllegalStateException("load-test 프로필에서는 web-push.sender.mode=simulated가 필요합니다.");
        }
    }

    @Bean
    NotificationSender notificationSender(
            ObjectMapper objectMapper,
            ObjectProvider<WebPushClient> webPushClient,
            WebPushNotificationClickTokenProvider clickTokenProvider
    ) {
        if (properties.mode() == Mode.SIMULATED) {
            log.warn("알림 시뮬레이터가 활성화되었습니다. 지연 시간={}ms. "
                            + "실제 Web Push 요청 없이 Delivery를 SENT로 처리합니다.",
                    properties.simulatedDelayMs());
            return new SimulatedNotificationSender(properties.simulatedDelayMs());
        }
        return new WebPushNotificationSender(objectMapper, webPushClient.getObject(), clickTokenProvider);
    }

    @Bean
    @ConditionalOnProperty(prefix = "web-push.sender", name = "mode", havingValue = "real", matchIfMissing = true)
    WebPushClient webPushClient(
            WebPushVapidProperties vapidProperties,
            NotificationDeliveryProperties deliveryProperties
    ) {
        return new WebPushClient(vapidProperties, deliveryProperties.sendTimeout());
    }
}
