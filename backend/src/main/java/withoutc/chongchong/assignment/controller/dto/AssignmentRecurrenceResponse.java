package withoutc.chongchong.assignment.controller.dto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import withoutc.chongchong.assignment.entity.AssignmentRecurrence;
import withoutc.chongchong.assignment.entity.RecurrenceSchedule;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;

public record AssignmentRecurrenceResponse(
        Long id,
        String title,
        String content,
        String submissionMethod,
        SubmissionVisibility submissionVisibility,
        SubmissionTarget submissionTarget,
        List<Long> submitterIds,
        int intervalWeeks,
        DayOfWeek dayOfWeek,
        LocalTime closeTime,
        LocalDateTime nextCloseAt,
        List<LocalDateTime> remindAts
) {

    public static AssignmentRecurrenceResponse from(AssignmentRecurrence recurrence) {
        RecurrenceSchedule schedule = recurrence.getSchedule();
        LocalDateTime nextCloseAt = schedule.nextCloseAt();
        return new AssignmentRecurrenceResponse(recurrence.getId(), recurrence.getTitle(), recurrence.getContent(),
                recurrence.getSubmissionMethod(), recurrence.getSubmissionVisibility(),
                recurrence.getSubmissionTarget(), recurrence.getSubmitterIds().stream().sorted().toList(),
                schedule.getIntervalWeeks(), nextCloseAt.getDayOfWeek(), schedule.getCloseTime(), nextCloseAt,
                recurrence.nextRemindAts());
    }
}
