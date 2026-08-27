package withoutc.chongchong.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;

public record StudyMemberResponse(
        @Schema(description = "스터디 멤버 ID", example = "1")
        Long id,
        @Schema(description = "스터디 내 멤버 이름", example = "홍길동")
        String name,
        @Schema(description = "스터디 멤버 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
        String profileImage,
        @Schema(description = "스터디 멤버 역할", example = "MEMBER")
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
