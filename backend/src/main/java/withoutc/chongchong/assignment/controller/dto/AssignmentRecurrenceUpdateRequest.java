package withoutc.chongchong.assignment.controller.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;

public record AssignmentRecurrenceUpdateRequest(
        @Size(max = 100, message = "제목은 {max}자 이내로 입력 가능합니다.")
        String title,
        @Size(max = 10000, message = "내용은 {max}자 이내로 입력 가능합니다.")
        String content,
        @Size(max = 10000, message = "제출 방법은 {max}자 이내로 입력 가능합니다.")
        String submissionMethod,
        SubmissionVisibility submissionVisibility,
        SubmissionTarget submissionTarget,
        @Size(min = 1, message = "제출할 스터디원을 한 명 이상 선택해 주세요.")
        List<
                @NotNull(message = "제출 대상 ID는 필수 값입니다.")
                @Positive(message = "제출 대상 ID는 양수여야 합니다.")
                        Long
                > submitterIds,
        @Future(message = "마감 시각은 현재보다 미래여야 합니다.")
        LocalDateTime nextCloseAt,
        @Min(value = 1, message = "반복 간격은 {value}주 이상이어야 합니다.")
        @Max(value = 4, message = "반복 간격은 {value}주 이하여야 합니다.")
        Integer intervalWeeks,
        List<@NotNull(message = "리마인드 시각은 필수 값입니다.") LocalDateTime> remindAts
) {
}
