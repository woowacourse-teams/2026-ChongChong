package withoutc.chongchong.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

class NotificationTest {

    private static final Study STUDY = mock(Study.class);
    private static final StudyMember RECIPIENT = mock(StudyMember.class);
    private static final Long RESOURCE_ID = 1L;

    @Test
    @DisplayName("알림을 생성하면 읽지 않은 리마인드 알림으로 생성된다")
    void createNotification() {
        Notification notification = Notification.create(
                STUDY,
                RECIPIENT,
                NotificationType.REMIND,
                RESOURCE_ID,
                NotificationResourceType.NOTICE
        );

        assertThat(notification.getStudy()).isSameAs(STUDY);
        assertThat(notification.getRecipient()).isSameAs(RECIPIENT);
        assertThat(notification.getType()).isEqualTo(NotificationType.REMIND);
        assertThat(notification.getResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.NOTICE);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("알림의 필수 값이 없으면 생성할 수 없다")
    void rejectMissingRequiredValues() {
        assertInvalidNotification(null, RECIPIENT, NotificationType.REMIND, RESOURCE_ID,
                NotificationResourceType.NOTICE);
        assertInvalidNotification(STUDY, null, NotificationType.REMIND, RESOURCE_ID,
                NotificationResourceType.NOTICE);
        assertInvalidNotification(STUDY, RECIPIENT, null, RESOURCE_ID, NotificationResourceType.NOTICE);
        assertInvalidNotification(STUDY, RECIPIENT, NotificationType.REMIND, null,
                NotificationResourceType.NOTICE);
        assertInvalidNotification(STUDY, RECIPIENT, NotificationType.REMIND, RESOURCE_ID, null);
    }

    private void assertInvalidNotification(
            Study study,
            StudyMember recipient,
            NotificationType type,
            Long resourceId,
            NotificationResourceType resourceType
    ) {
        assertThatThrownBy(() -> Notification.create(study, recipient, type, resourceId, resourceType))
                .isInstanceOf(NotificationException.class)
                .extracting(exception -> ((NotificationException) exception).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION);
    }
}
