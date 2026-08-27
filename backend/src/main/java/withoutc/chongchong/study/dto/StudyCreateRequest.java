package withoutc.chongchong.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import withoutc.chongchong.study.entity.Study;

public record StudyCreateRequest(
        @NotBlank(message = "스터디 이름은 필수입니다.")
        @Size(max = 15, message = "스터디 이름은 15자 이내여야 합니다.")
        @Schema(description = "스터디 이름", example = "자바 스터디")
        String name,

        @Size(max = 30, message = "스터디 설명은 30자 이내여야 합니다.")
        @Schema(description = "스터디 설명", example = "매주 월요일에 진행한다.")
        String description
) {

    public Study toStudy() {
        return Study.create(name, description);
    }
}
