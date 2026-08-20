package withoutc.chongchong.study.dto;

import java.util.List;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMemberRole;

public record MyStudyListResponse(
        int studyCount,
        List<MyStudyResponse> studies
) {

    public record MyStudyResponse(
            Long id,
            StudyMemberRole role,
            String name,
            String description,
            int memberCount,
            int noticeCount,
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

