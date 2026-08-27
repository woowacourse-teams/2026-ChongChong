package withoutc.chongchong.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMemberRole;

public record MyStudyListResponse(
        @Schema(description = "가입한 스터디 수", example = "2")
        int studyCount,
        @Schema(description = "가입한 스터디 목록")
        List<MyStudyResponse> studies
) {

    public record MyStudyResponse(
            @Schema(description = "스터디 ID", example = "1")
            Long id,
            @Schema(description = "현재 사용자의 스터디 역할", example = "MEMBER")
            StudyMemberRole role,
            @Schema(description = "스터디 이름", example = "자바 스터디")
            String name,
            @Schema(description = "스터디 설명", example = "매주 월요일에 진행한다.")
            String description,
            @Schema(description = "스터디 멤버 수", example = "5")
            int memberCount,
            @Schema(description = "공지 수", example = "3")
            int noticeCount,
            @Schema(description = "과제 수", example = "4")
            int assignmentCount
    ) {

        public static MyStudyResponse from(Study study, StudyMemberRole role, int memberCount, int noticeCount,
                                           int assignmentCount) {
            return new MyStudyResponse(
                    study.getId(),
                    role,
                    study.getName(),
                    study.getDescription(),
                    memberCount,
                    noticeCount,
                    assignmentCount
            );
        }
    }
}
