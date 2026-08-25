package withoutc.chongchong.assignment.controller.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentUpdateRequest(
        @Size(max = 15, message = "제목은 15자 이내로 입력 가능합니다.")
        String title,
        @Size(max = 10000, message = "내용은 10,000자 이내로 입력 가능합니다.")
        String content,
        @Size(max = 10000, message = "제출 방법은 10,000자 이내로 입력 가능합니다.")
        String submissionMethod,
        @Future(message = "마감 시각은 현재보다 미래여야 합니다.")
        LocalDateTime closeAt,
        List<
                @NotNull(message = "리마인드 시각은 필수 값 입니다.")
                @Future(message = "리마인드 시각은 현재보다 미래여야 합니다.")
                        LocalDateTime
                > remindAts
) {
}
