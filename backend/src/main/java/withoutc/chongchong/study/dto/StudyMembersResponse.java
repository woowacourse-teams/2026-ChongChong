package withoutc.chongchong.study.dto;

import java.util.List;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;

public record StudyMembersResponse(
        List<StudyMemberResponse> members
) {

    public static StudyMembersResponse from(
            List<StudyMemberSummaryProjection> projections
    ) {
        List<StudyMemberResponse> members = projections.stream()
                .map(StudyMemberResponse::from)
                .toList();

        return new StudyMembersResponse(members);
    }
}
