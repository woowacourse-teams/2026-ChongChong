package withoutc.chongchong.study.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyCreateRequest(
        @NotBlank(message = "스터디 이름은 필수입니다.")
        @Size(max = 15, message = "스터디 이름은 15자 이내여야 합니다.")
        String name,

        @Size(max = 30, message = "스터디 설명은 30자 이내여야 합니다.")
        String description
) {
}
