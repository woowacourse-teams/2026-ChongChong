package withoutc.chongchong.notification.sender;

import java.util.List;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.study.entity.StudyMember;

public record NotificationEvent(
        NotificationType type,
        Long resourceId,
        NotificationResourceType resourceType,
        Long studyId,
        String content,
        List<Recipient> recipients
) {

    public static NotificationEvent create(
            NotificationType type,
            Long resourceId,
            NotificationResourceType resourceType,
            Long studyId,
            String content,
            List<StudyMember> recipients
    ) {
        return new NotificationEvent(type, resourceId, resourceType, studyId, content,
                recipients.stream().map(Recipient::create).toList());
    }

    public record Recipient(
            Long id,
            String name
    ) {

        static Recipient create(StudyMember member) {
            return new Recipient(member.getId(), member.getName());
        }
    }
}
