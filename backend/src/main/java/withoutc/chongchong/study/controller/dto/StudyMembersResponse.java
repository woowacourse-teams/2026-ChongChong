package withoutc.chongchong.study.controller.dto;

import java.util.List;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;

public record StudyMembersResponse(
        int maxMemberCount,
        int nowMemberCount,
        List<StudyMemberResponse> members
) {

    public static StudyMembersResponse from(
            int maxMemberCount,
            List<StudyMemberSummaryProjection> projections
    ) {
        List<StudyMemberResponse> members = projections.stream()
                .map(StudyMemberResponse::from)
                .toList();

        return new StudyMembersResponse(maxMemberCount, members.size(), members);
    }
}
