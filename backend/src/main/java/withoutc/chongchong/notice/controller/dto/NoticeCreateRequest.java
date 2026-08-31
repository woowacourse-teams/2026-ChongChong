package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeCreateRequest(
        @NotBlank(message = "제목은 필수 값입니다.")
        @Size(max = 20, message = "제목은 20자 이내로 입력 가능합니다.")
        @Schema(description = "공지 제목", example = "이번 주 공지")
        String title,
        @NotBlank(message = "내용은 필수 값입니다.")
        @Size(max = 10000, message = "내용은 10,000자 이내로 입력 가능합니다.")
        @Schema(description = "공지 내용", example = "이번 주 스터디 일정을 확인해주세요.")
        String content,
        @Schema(description = "리마인드 예정 시각 목록", example = "[\"2026-08-28T10:00:00\"]")
        List<
                @NotNull(message = "리마인드 시각은 필수 값입니다.")
                @Future(message = "리마인드 시각은 현재보다 미래여야 합니다.")
                        LocalDateTime
                > remindAts
) {
}
