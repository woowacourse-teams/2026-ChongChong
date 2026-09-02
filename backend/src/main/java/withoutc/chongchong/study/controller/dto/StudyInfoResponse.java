package withoutc.chongchong.study.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import withoutc.chongchong.study.entity.StudyMemberRole;

public record StudyInfoResponse(
        @Schema(description = "스터디 이름", example = "자바 스터디")
        String studyName,
        @Schema(description = "현재 사용자의 스터디 역할", example = "LEADER")
        StudyMemberRole role,
        @Schema(description = "현재 사용자 이름", example = "홍길동")
        String userName
) {
}
