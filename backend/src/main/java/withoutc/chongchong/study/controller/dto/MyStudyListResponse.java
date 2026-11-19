package withoutc.chongchong.study.controller.dto;

import java.util.List;
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
            boolean isPublic,
            int memberCount,
            int noticeCount,
            int assignmentCount,
            String imageUrl
    ) {

    }
}
