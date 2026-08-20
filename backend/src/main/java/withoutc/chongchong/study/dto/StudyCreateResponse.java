package withoutc.chongchong.study.dto;

import withoutc.chongchong.study.entity.Study;

public record StudyCreateResponse(
        Long id
) {

    public static StudyCreateResponse from(Study study) {
        return new StudyCreateResponse(study.getId());
    }
}
