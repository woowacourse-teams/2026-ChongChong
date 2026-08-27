package withoutc.chongchong.study.dto;

import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;

public record StudyMemberResponse(
        Long id,
        String name,
        String profileImage,
        StudyMemberRole role
) {

    public static StudyMemberResponse from(
            StudyMemberSummaryProjection projection
    ) {
        return new StudyMemberResponse(
                projection.id(),
                projection.name(),
                projection.profileImageUrl(),
                projection.role()
        );
    }
}
