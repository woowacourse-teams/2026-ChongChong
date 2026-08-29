package withoutc.chongchong.study.repository.projection;

import withoutc.chongchong.study.entity.StudyMemberRole;

public record StudyMemberSummaryProjection(
        Long id,
        String name,
        String profileImageUrl,
        StudyMemberRole role
) {
}
