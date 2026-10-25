package withoutc.chongchong.file.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PresignedUrlCreateRequest(
        @NotBlank(message = "원본 파일명은 필수 값입니다.")
        @Size(max = 255, message = "원본 파일명은 {max}자 이내로 입력 가능합니다.")
        String originalFileName,
        @NotBlank(message = "파일 유형은 필수 값입니다.")
        @Size(max = 255, message = "파일 유형은 {max}자 이내로 입력 가능합니다.")
        String contentType,
        @NotNull(message = "파일 크기는 필수 값입니다.")
        @PositiveOrZero(message = "파일 크기는 0바이트 이상이어야 합니다.")
        Long size
) {
}
