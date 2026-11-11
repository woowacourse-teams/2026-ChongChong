package withoutc.chongchong.notification.repository.projection;

import withoutc.chongchong.notification.entity.ResourceType;

public record ReminderCandidateProjection(
        ResourceType resourceType,
        Long reminderId,
        Long resourceId,
        Long studyId
) {
}
