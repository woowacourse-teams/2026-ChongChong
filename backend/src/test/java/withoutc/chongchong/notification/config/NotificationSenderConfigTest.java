package withoutc.chongchong.notification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import withoutc.chongchong.notification.sender.NotificationSender;
import withoutc.chongchong.notification.sender.SimulatedNotificationSender;
import withoutc.chongchong.notification.sender.WebPushClient;
import withoutc.chongchong.notification.sender.WebPushNotificationSender;
import withoutc.chongchong.notification.token.WebPushNotificationClickTokenProvider;

class NotificationSenderConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(WebPushVapidConfig.class, NotificationSenderConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(WebPushNotificationClickTokenProvider.class,
                    () -> mock(WebPushNotificationClickTokenProvider.class))
            .withPropertyValues(
                    "web-push.vapid.public-key="
                            + "BDcLQBzrmGYpdNmsaQrk9_uU9Q_wQb2ziEN5AIcoUq4bi2uKlC2UKcmfBuUdJj_NzcDu-ldeUpif_na9i-u3HKw",
                    "web-push.vapid.private-key=67yyNB109eqWbw7r3HcJCuwmLDaDROaI6DJH4nCWUYU",
                    "web-push.vapid.subject=mailto:test@example.com"
            );

    @Test
    @DisplayName("모드를 생략한 prod 환경은 실제 Web Push sender만 등록한다")
    void useRealSenderByDefault() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues("app.environment=prod")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(NotificationSender.class);
                    assertThat(context.getBean(NotificationSender.class)).isInstanceOf(WebPushNotificationSender.class);
                    assertThat(context).hasSingleBean(WebPushClient.class);
                    assertThat(context).doesNotHaveBean(SimulatedNotificationSender.class);
                });
    }

    @Test
    @DisplayName("명시적인 real 모드는 기존 test 프로필에서도 실제 sender를 등록한다")
    void useExplicitRealSender() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
                .withPropertyValues("web-push.sender.mode=real")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(NotificationSender.class);
                    assertThat(context.getBean(NotificationSender.class)).isInstanceOf(WebPushNotificationSender.class);
                });
    }

    @Test
    @DisplayName("dev의 load-test 프로필과 simulated 모드를 함께 지정하면 실제 전송 client를 생성하지 않는다")
    void useSimulatorWithoutRealClient() {
        simulatedContextRunner()
                .withPropertyValues(
                        "web-push.sender.simulated-delay-ms=0",
                        "web-push.vapid.public-key=not-a-real-vapid-key",
                        "web-push.vapid.private-key=not-a-real-vapid-key"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(NotificationSender.class);
                    assertThat(context.getBean(NotificationSender.class)).isInstanceOf(SimulatedNotificationSender.class);
                    assertThat(context).doesNotHaveBean(WebPushClient.class);
                    assertThat(context).doesNotHaveBean(WebPushNotificationSender.class);
                });
    }

    @Test
    @DisplayName("시뮬레이터의 지연 시간을 생략하면 500ms로 설정한다")
    void useDefaultSimulatedDelay() {
        simulatedContextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(NotificationSenderProperties.class).simulatedDelayMs()).isEqualTo(500);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "local", "test", "staging", ""})
    @DisplayName("dev가 아닌 환경의 simulated 모드는 애플리케이션 시작을 실패시킨다")
    void rejectSimulatorOutsideDev(String environment) {
        simulatedContextRunner()
                .withPropertyValues("app.environment=" + environment)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "알림 시뮬레이터는 prod 프로필 없이 load-test 프로필과 app.environment=dev에서만 사용할 수 있습니다.");
                });
    }

    @Test
    @DisplayName("환경 표시가 누락된 simulated 모드는 애플리케이션 시작을 실패시킨다")
    void rejectSimulatorWithoutEnvironment() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("load-test"))
                .withPropertyValues("web-push.sender.mode=simulated")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("dev에서도 load-test 프로필이 없으면 시뮬레이터를 시작하지 않는다")
    void rejectSimulatorWithoutLoadTestProfile() {
        contextRunner
                .withPropertyValues("app.environment=dev", "web-push.sender.mode=simulated")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("환경 표시가 dev여도 prod 프로필이 함께 활성화되면 시뮬레이터를 시작하지 않는다")
    void rejectSimulatorWithProdProfile() {
        simulatedContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("load-test", "prod"))
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"real", ""})
    @DisplayName("load-test 프로필에서 simulated 모드를 명시하지 않으면 실제 전송으로 대체하지 않고 시작을 실패시킨다")
    void rejectRealSenderInLoadTestProfile(String mode) {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("load-test"))
                .withPropertyValues("app.environment=dev", "web-push.sender.mode=" + mode)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("load-test 프로필에서 모드를 생략하면 실제 전송으로 대체하지 않고 시작을 실패시킨다")
    void rejectDefaultModeInLoadTestProfile() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("load-test"))
                .withPropertyValues("app.environment=dev")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("알 수 없는 모드는 애플리케이션 시작을 실패시킨다")
    void rejectUnknownMode() {
        contextRunner
                .withPropertyValues("web-push.sender.mode=simulation")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("음수인 시뮬레이터 지연 시간은 애플리케이션 시작을 실패시킨다")
    void rejectNegativeDelay() {
        simulatedContextRunner()
                .withPropertyValues("web-push.sender.simulated-delay-ms=-1")
                .run(context -> assertThat(context).hasFailed());
    }

    private ApplicationContextRunner simulatedContextRunner() {
        return contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("load-test"))
                .withPropertyValues("app.environment=dev", "web-push.sender.mode=simulated");
    }
}
