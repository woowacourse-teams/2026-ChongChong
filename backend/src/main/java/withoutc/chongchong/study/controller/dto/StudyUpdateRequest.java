package withoutc.chongchong.study.controller.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StudyUpdateRequest(
        @Size(max = 15, message = "스터디 이름은 {max}자 이내여야 합니다.")
        String name,

        @Size(max = 30, message = "스터디 설명은 {max}자 이내여야 합니다.")
        String description,

        Boolean isPublic,

        @Positive(message = "스터디 이미지 파일 ID는 양수여야 합니다.")
        Long imageFileId
) {
}
