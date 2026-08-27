package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentCreateRequest(
        @NotBlank(message = "제목은 필수 값입니다.")
        @Size(max = 15, message = "제목은 15자 이내로 입력 가능합니다.")
        @Schema(description = "과제 제목", example = "1주 차 과제")
        String title,
        @NotBlank(message = "내용은 필수 값입니다.")
        @Size(max = 10000, message = "내용은 10,000자 이내로 입력 가능합니다.")
        @Schema(description = "과제 내용", example = "이번 주 과제를 제출해주세요.")
        String content,
        @NotBlank(message = "제출 방법은 필수 값입니다.")
        @Size(max = 10000, message = "제출 방법은 10,000자 이내로 입력 가능합니다.")
        @Schema(description = "과제 제출 방법", example = "링크 제출")
        String submissionMethod,
        @NotNull(message = "마감 시각은 필수 값입니다.")
        @Future(message = "마감 시각은 현재보다 미래여야 합니다.")
        @Schema(description = "과제 마감 시각", example = "2099-12-31T23:59:00")
        LocalDateTime closeAt,
        @Schema(description = "리마인드 예정 시각 목록", example = "[\"2099-12-30T10:00:00\"]")
        List<
                @NotNull(message = "리마인드 시각은 필수 값입니다.")
                @Future(message = "리마인드 시각은 현재보다 미래여야 합니다.")
                        LocalDateTime
                > remindAts

) {
}
