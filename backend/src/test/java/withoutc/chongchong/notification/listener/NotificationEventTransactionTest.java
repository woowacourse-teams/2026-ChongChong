package withoutc.chongchong.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.notification.support.TestNotificationSender;
import withoutc.chongchong.notification.support.TestNotificationSenderConfiguration;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestNotificationSenderConfiguration.class)
class NotificationEventTransactionTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestNotificationSender notificationSender;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        notificationSender.clear();
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("트랜잭션이 커밋된 후 알림을 전송한다")
    void sendsAfterCommit() {
        NotificationEvent event = createEvent();

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        assertThat(notificationSender.events()).containsExactly(event);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 알림을 전송하지 않는다")
    void doesNotSendAfterRollback() {
        NotificationEvent event = createEvent();

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        assertThat(notificationSender.events()).isEmpty();
    }

    @Test
    @DisplayName("Sender 전송 실패가 커밋된 트랜잭션의 호출자에게 전파되지 않는다")
    void isolatesSenderFailureAfterCommit() {
        notificationSender.failWith(new ResourceAccessException("Discord unavailable"));

        assertThatCode(() -> transactionTemplate.executeWithoutResult(
                status -> eventPublisher.publishEvent(createEvent())
        )).doesNotThrowAnyException();
    }

    private NotificationEvent createEvent() {
        return new NotificationEvent(
                NotificationType.CREATED,
                1L,
                NotificationResourceType.NOTICE,
                1L,
                "공지 제목",
                List.of(new NotificationEvent.Recipient(2L, "스터디원"))
        );
    }
}
