package withoutc.chongchong.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import withoutc.chongchong.study.entity.Study;

public record StudyCreateRequest(
        // TBD: 스터디 이름에 허용할 문자 범위 결정
        @NotBlank
        @Size(max = 15)
        String name,

        @Size(max = 30)
        String description
) {

    public Study toStudy() {
        return Study.create(name, description);
    }
}
