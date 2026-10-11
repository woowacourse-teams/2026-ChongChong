package withoutc.chongchong.notification.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestNotificationSenderConfiguration {

    @Bean
    @Primary
    TestNotificationSender testNotificationSender() {
        return new TestNotificationSender();
    }
}
