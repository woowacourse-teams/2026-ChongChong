package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeUpdateRequest(
        @Size(max = 15, message = "제목은 15자 이내로 입력 가능합니다.")
        @Schema(description = "수정할 공지 제목", example = "수정된 공지")
        String title,
        @Size(max = 10000, message = "내용은 10,000자 이내로 입력 가능합니다.")
        @Schema(description = "수정할 공지 내용", example = "수정된 공지 내용입니다.")
        String content,
        @Schema(description = "수정할 리마인드 예정 시각 목록", example = "[\"2099-12-30T10:00:00\"]")
        List<
                @NotNull(message = "리마인드 시각은 null일 수 없습니다.")
                @Future(message = "리마인드 시각은 미래여야 합니다.")
                        LocalDateTime
                > remindAts
) {
}
