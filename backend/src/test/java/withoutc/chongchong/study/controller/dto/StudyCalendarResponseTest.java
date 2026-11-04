package withoutc.chongchong.study.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import withoutc.chongchong.assignment.entity.SubmissionStatus;
import withoutc.chongchong.notice.entity.NoticeReadStatus;
import withoutc.chongchong.study.entity.StudyCalendarEventType;

class StudyCalendarResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("공지에는 readStatus만, 과제에는 submissionStatus만 직렬화하여 이벤트 계약을 구분한다")
    void serializesOnlyApplicableStatusFields() {
        LocalDateTime eventAt = LocalDateTime.of(2026, 11, 1, 9, 0);
        var response = new StudyCalendarResponse(2026, 11, List.of(
                StudyCalendarEventResponse.notice(1L, "공지", eventAt, NoticeReadStatus.NOT_ASSIGNED),
                StudyCalendarEventResponse.assignment(StudyCalendarEventType.ASSIGNMENT_CREATED, 2L,
                        "과제", eventAt, SubmissionStatus.SUBMITTED),
                StudyCalendarEventResponse.assignment(StudyCalendarEventType.ASSIGNMENT_DUE, 2L,
                        "과제", eventAt.plusDays(1), SubmissionStatus.SUBMITTED)));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.get("year").asInt()).isEqualTo(2026);
        assertThat(json.get("month").asInt()).isEqualTo(11);
        JsonNode events = json.get("events");
        assertThat(events.size()).isEqualTo(3);
        assertThat(events.get(0).size()).isEqualTo(6);
        assertThat(events.get(0).get("readStatus").asString()).isEqualTo("NOT_ASSIGNED");
        assertThat(events.get(0).has("submissionStatus")).isFalse();
        assertThat(events.get(0).get("resourceType").asString()).isEqualTo("NOTICE");
        assertThat(events.get(0).get("eventAt").asString()).isEqualTo("2026-11-01T09:00:00");
        assertThat(events.get(1).size()).isEqualTo(6);
        assertThat(events.get(1).get("submissionStatus").asString()).isEqualTo("SUBMITTED");
        assertThat(events.get(1).has("readStatus")).isFalse();
        assertThat(events.get(1).get("resourceType").asString()).isEqualTo("ASSIGNMENT");
        assertThat(events.get(2).get("eventType").asString()).isEqualTo("ASSIGNMENT_DUE");
        assertThat(events.get(2).get("resourceId").asLong()).isEqualTo(2L);
        assertThat(events.get(2).has("readStatus")).isFalse();
    }

    @Test
    @DisplayName("일정이 없는 월도 연·월과 빈 events 배열을 가진 JSON 객체로 반환한다")
    void serializesEmptyEventsAsArray() {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(new StudyCalendarResponse(2026, 11, List.of())));

        assertThat(json.get("year").asInt()).isEqualTo(2026);
        assertThat(json.get("month").asInt()).isEqualTo(11);
        assertThat(json.get("events").isArray()).isTrue();
        assertThat(json.get("events").isEmpty()).isTrue();
    }
}
