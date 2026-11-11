package withoutc.chongchong.study.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.study.controller.dto.StudyCalendarResponse;
import withoutc.chongchong.study.service.StudyCalendarService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/calendar")
public class StudyCalendarController {

    private final StudyCalendarService studyCalendarService;

    @GetMapping
    public ResponseEntity<StudyCalendarResponse> getStudyCalendar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId,
            @RequestParam @Min(value = 1, message = "year는 1 이상이어야 합니다.")
            @Max(value = 9998, message = "year는 9998 이하여야 합니다.") int year,
            @RequestParam @Min(value = 1, message = "month는 1 이상이어야 합니다.")
            @Max(value = 12, message = "month는 12 이하여야 합니다.") int month
    ) {
        return ResponseEntity.ok(studyCalendarService.getCalendar(user.id(), studyId, year, month));
    }
}
