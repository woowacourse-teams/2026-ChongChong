package withoutc.chongchong.notice.controller.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeUpdateRequest(
        @Size(max = 100, message = "제목은 {max}자 이내로 입력 가능합니다.")
        String title,
        @Size(max = 10000, message = "내용은 {max}자 이내로 입력 가능합니다.")
        String content,
        List<
                @NotNull(message = "리마인드 시각은 null일 수 없습니다.")
                @Future(message = "리마인드 시각은 미래여야 합니다.")
                        LocalDateTime
                > remindAts,
        List<
                @NotNull(message = "첨부 파일 ID는 null일 수 없습니다.")
                @Positive(message = "첨부 파일 ID는 양수여야 합니다.")
                        Long
                > attachmentFileIds
) {
}
