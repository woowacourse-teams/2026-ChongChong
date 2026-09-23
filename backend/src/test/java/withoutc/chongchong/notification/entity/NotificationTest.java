package withoutc.chongchong.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;
import withoutc.chongchong.user.entity.User;

class NotificationTest {

    private static final User RECIPIENT = mock(User.class);
    private static final String TITLE = "[스터디] 새 공지";
    private static final String BODY = "공지 제목";
    private static final Long RESOURCE_ID = 1L;
    private static final String DEEP_LINK = "/studies/1/notices/1";

    @Test
    @DisplayName("알림을 생성하면 읽지 않은 리마인드 알림으로 생성된다")
    void createNotification() {
        Notification notification = Notification.create(
                RECIPIENT,
                TITLE,
                BODY,
                NotificationType.REMIND,
                RESOURCE_ID,
                ResourceType.NOTICE,
                DEEP_LINK
        );

        assertThat(notification.getRecipient()).isSameAs(RECIPIENT);
        assertThat(notification.getTitle()).isEqualTo(TITLE);
        assertThat(notification.getBody()).isEqualTo(BODY);
        assertThat(notification.getType()).isEqualTo(NotificationType.REMIND);
        assertThat(notification.getResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(notification.getResourceType()).isEqualTo(ResourceType.NOTICE);
        assertThat(notification.getDeepLink()).isEqualTo(DEEP_LINK);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("알림의 필수 값이 없으면 생성할 수 없다")
    void rejectMissingRequiredValues() {
        assertInvalidNotification(null, TITLE, BODY, NotificationType.REMIND, RESOURCE_ID,
                ResourceType.NOTICE, DEEP_LINK);
        assertInvalidNotification(RECIPIENT, null, BODY, NotificationType.REMIND, RESOURCE_ID,
                ResourceType.NOTICE, DEEP_LINK);
        assertInvalidNotification(RECIPIENT, TITLE, null, NotificationType.REMIND, RESOURCE_ID,
                ResourceType.NOTICE, DEEP_LINK);
        assertInvalidNotification(RECIPIENT, TITLE, BODY, null, RESOURCE_ID,
                ResourceType.NOTICE, DEEP_LINK);
        assertInvalidNotification(RECIPIENT, TITLE, BODY, NotificationType.REMIND, null,
                ResourceType.NOTICE, DEEP_LINK);
        assertInvalidNotification(RECIPIENT, TITLE, BODY, NotificationType.REMIND, RESOURCE_ID, null,
                DEEP_LINK);
        assertInvalidNotification(RECIPIENT, TITLE, BODY, NotificationType.REMIND, RESOURCE_ID,
                ResourceType.NOTICE, null);
        assertInvalidNotification(RECIPIENT, " ", BODY, NotificationType.REMIND, RESOURCE_ID,
                ResourceType.NOTICE, DEEP_LINK);
        assertInvalidNotification(RECIPIENT, TITLE, " ", NotificationType.REMIND, RESOURCE_ID,
                ResourceType.NOTICE, DEEP_LINK);
    }

    private void assertInvalidNotification(
            User recipient,
            String title,
            String body,
            NotificationType type,
            Long resourceId,
            ResourceType resourceType,
            String deepLink
    ) {
        assertThatThrownBy(() -> Notification.create(
                recipient, title, body, type, resourceId, resourceType, deepLink))
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION);
    }
}
