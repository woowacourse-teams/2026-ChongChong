package withoutc.chongchong.assignment.controller.dto;

import jakarta.validation.constraints.Size;

public record AssignmentSubmitRequest(
        @Size(max = 10000, message = "내용은 10,000 이내로 입력 가능합니다.")
        String content,
        @Size(max = 10000, message = "링크는 10,000 이내로 입력 가능합니다.")
        String link
) {
}
