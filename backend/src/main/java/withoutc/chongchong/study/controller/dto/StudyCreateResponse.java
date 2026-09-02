package withoutc.chongchong.study.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import withoutc.chongchong.study.entity.Study;

public record StudyCreateResponse(
        @Schema(description = "생성된 스터디 ID", example = "1")
        Long studyId
) {

    public static StudyCreateResponse from(Study study) {
        return new StudyCreateResponse(study.getId());
    }
}
