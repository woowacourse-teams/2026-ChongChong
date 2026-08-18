package withoutc.chongchong.notice.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeCreateRequest(
        @NotBlank(message = "제목은 필수 값입니다.")
        String title,
        @NotBlank(message = "내용은 필수 값입니다.")
        String content,
        List<LocalDateTime> remindAts
) {
}
