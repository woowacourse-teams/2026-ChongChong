package withoutc.chongchong.notification.sender;

import java.util.List;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.entity.StudyMember;

public record NotificationEvent(
        String title,
        String body,
        NotificationType type,
        Long resourceId,
        ResourceType resourceType,
        String deepLink,
        List<Recipient> recipients
) {

    public static NotificationEvent create(
            String title,
            String body,
            NotificationType type,
            Long resourceId,
            ResourceType resourceType,
            String deepLink,
            List<StudyMember> recipients
    ) {
        return new NotificationEvent(title, body, type, resourceId, resourceType, deepLink,
                recipients.stream().map(Recipient::create).toList());
    }

    public record Recipient(
            Long userId,
            String memberName
    ) {

        static Recipient create(StudyMember member) {
            return new Recipient(member.getUser().getId(), member.getName());
        }
    }
}
