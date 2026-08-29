package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record AssignmentSubmitRequest(
        @Size(max = 10000, message = "내용은 10,000 이내로 입력 가능합니다.")
        @Schema(description = "제출 내용", example = "이번 주 과제를 완료했습니다.")
        String content,
        @Size(max = 10000, message = "링크는 10,000 이내로 입력 가능합니다.")
        @Schema(description = "제출 링크", example = "https://github.com/example/project")
        String link
) {
}
