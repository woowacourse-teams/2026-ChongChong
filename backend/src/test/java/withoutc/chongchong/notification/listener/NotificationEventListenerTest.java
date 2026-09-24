package withoutc.chongchong.notification.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.sender.NotificationEvent;
import withoutc.chongchong.notification.sender.NotificationSender;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationSender notificationSender;

    @Test
    @DisplayName("알림 이벤트를 받으면 Sender에 전달한다")
    void handle() {
        NotificationEventListener listener = new NotificationEventListener(notificationSender);
        NotificationEvent event = createEvent();

        listener.handle(event);

        verify(notificationSender).sendNotifications(event);
    }

    @Test
    @DisplayName("Sender 전송 실패가 예외로 전파되지 않는다")
    void handleIgnoresSenderFailure() {
        NotificationEventListener listener = new NotificationEventListener(notificationSender);
        NotificationEvent event = createEvent();
        doThrow(new ResourceAccessException("Discord unavailable"))
                .when(notificationSender)
                .sendNotifications(event);

        assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
    }

    private NotificationEvent createEvent() {
        return new NotificationEvent(
                "[스터디] 새 공지",
                "공지 제목",
                NotificationType.NEW,
                1L,
                ResourceType.NOTICE,
                "/studies/1/notices/1",
                List.of()
        );
    }
}
