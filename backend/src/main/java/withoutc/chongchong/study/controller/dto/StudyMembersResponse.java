package withoutc.chongchong.study.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;

public record StudyMembersResponse(
        @Schema(description = "스터디 멤버 목록")
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
