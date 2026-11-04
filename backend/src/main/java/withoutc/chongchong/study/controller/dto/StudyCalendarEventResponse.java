package withoutc.chongchong.study.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.SubmissionStatus;
import withoutc.chongchong.notice.entity.NoticeReadStatus;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.entity.StudyCalendarEventType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudyCalendarEventResponse(
        StudyCalendarEventType eventType,
        ResourceType resourceType,
        Long resourceId,
        String title,
        LocalDateTime eventAt,
        NoticeReadStatus readStatus,
        SubmissionStatus submissionStatus
) {
    public static StudyCalendarEventResponse notice(Long id, String title, LocalDateTime createdAt,
                                                    NoticeReadStatus readStatus) {
        return new StudyCalendarEventResponse(StudyCalendarEventType.NOTICE_CREATED, ResourceType.NOTICE,
                id, title, createdAt, readStatus, null);
    }

    public static StudyCalendarEventResponse assignment(StudyCalendarEventType eventType, Long id, String title,
                                                        LocalDateTime eventAt, SubmissionStatus submissionStatus) {
        return new StudyCalendarEventResponse(eventType, ResourceType.ASSIGNMENT,
                id, title, eventAt, null, submissionStatus);
    }
}
