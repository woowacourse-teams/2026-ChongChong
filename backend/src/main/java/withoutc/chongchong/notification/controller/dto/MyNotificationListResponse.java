package withoutc.chongchong.notification.controller.dto;

import java.time.LocalDateTime;
import java.util.List;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;

public record MyNotificationListResponse(
        Long nextCursor,
        boolean hasNext,
        List<MyNotificationResponse> notifications
) {

    private record MyNotificationResponse(
            Long id,
            String title,
            String body,
            NotificationType type,
            ResourceType resourceType,
            Long resourceId,
            String deepLink,
            boolean isRead,
            LocalDateTime createdAt
    ) {

        private static MyNotificationResponse from(Notification notification) {
            return new MyNotificationResponse(
                    notification.getId(),
                    notification.getTitle(),
                    notification.getBody(),
                    notification.getType(),
                    notification.getResourceType(),
                    notification.getResourceId(),
                    notification.getDeepLink(),
                    notification.isRead(),
                    notification.getCreatedAt()
            );
        }
    }

    public static MyNotificationListResponse from(CursorPageResponse<Notification> notificationPage) {
        List<MyNotificationResponse> response = notificationPage.content().stream()
                .map(MyNotificationResponse::from)
                .toList();

        return new MyNotificationListResponse(notificationPage.nextCursor(), notificationPage.hasNext(), response);
    }
}
