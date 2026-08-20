package withoutc.chongchong.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeCreateRequest(
        @NotBlank(message = "제목은 필수 값입니다.")
        @Size(max = 15, message = "제목은 15자 이내로 입력 가능합니다.")
        String title,
        @NotBlank(message = "내용은 필수 값입니다.")
        @Size(max = 10000, message = "내용은 10,000자 이내로 입력 가능합니다.")
        String content,
        List<LocalDateTime> remindAts
) {
}
