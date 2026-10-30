package withoutc.chongchong.study.controller.dto;

import jakarta.validation.constraints.Positive;

public record StudyJoinRequest(
        @Positive(message = "스터디 ID는 양수여야 합니다.")
        Long studyId,
        String token
) {
}
