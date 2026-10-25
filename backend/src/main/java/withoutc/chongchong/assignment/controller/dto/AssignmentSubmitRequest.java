package withoutc.chongchong.assignment.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AssignmentSubmitRequest(
        @Size(max = 10000, message = "내용은 {max}자 이내로 입력 가능합니다.")
        String content,
        @Size(max = 10000, message = "링크는 {max}자 이내로 입력 가능합니다.")
        String link,
        List<
                @NotNull(message = "첨부파일 ID는 필수 값입니다.")
                @Positive(message = "첨부파일 ID는 양수여야 합니다.")
                        Long
                > attachmentFileIds
) {
    public AssignmentSubmitRequest(String content, String link) {
        this(content, link, null);
    }
}
