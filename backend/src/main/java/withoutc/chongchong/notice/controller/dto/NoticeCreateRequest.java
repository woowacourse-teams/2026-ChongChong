package withoutc.chongchong.notice.controller.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        List<
                @NotNull(message = "리마인드 시각은 필수 값입니다.")
                @Future(message = "리마인드 시각은 현재보다 미래여야 합니다.")
                        LocalDateTime
                > remindAts
) {
}
