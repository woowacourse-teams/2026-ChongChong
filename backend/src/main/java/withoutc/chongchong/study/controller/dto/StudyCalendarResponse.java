package withoutc.chongchong.study.controller.dto;

import java.util.List;

public record StudyCalendarResponse(
        int year,
        int month,
        List<StudyCalendarEventResponse> events
) {
}
