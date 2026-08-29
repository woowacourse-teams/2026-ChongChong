package withoutc.chongchong.assignment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentStatusesResponse(
        @Schema(description = "과제 ID", example = "1")
        Long id,
        @Schema(description = "과제 대상 멤버 수", example = "5")
        int memberCount,
        @Schema(description = "과제 제출 완료 멤버 수", example = "3")
        int completeCount,
        @Schema(description = "과제 미제출 멤버 수", example = "2")
        int incompleteCount,
        @Schema(description = "다음 리마인드 예정 시각", example = "2026-08-28T10:00:00", nullable = true)
        LocalDateTime remindAt,
        @Schema(description = "제출 완료 멤버 목록")
        List<CompleteMember> completeMembers,
        @Schema(description = "미제출 멤버 목록")
        List<IncompleteMember> incompleteMembers
) {
    public static AssignmentStatusesResponse of(
            Long assignmentId,
            LocalDateTime remindAt,
            List<CompleteMember> completeMembers,
            List<IncompleteMember> incompleteMembers
    ) {
        return new AssignmentStatusesResponse(
                assignmentId,
                completeMembers.size() + incompleteMembers.size(),
                completeMembers.size(),
                incompleteMembers.size(),
                remindAt,
                completeMembers,
                incompleteMembers
        );
    }


    public record CompleteMember(
            @Schema(description = "스터디 멤버 ID", example = "1")
            Long id,
            @Schema(description = "멤버 이름", example = "홍길동")
            String name,
            @Schema(description = "멤버 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
            String profileImage
    ) {
        public static CompleteMember of(Long studyMemberId, String name, String profileImageUrl) {
            return new CompleteMember(studyMemberId, name, profileImageUrl);
        }

    }

    public record IncompleteMember(
            @Schema(description = "스터디 멤버 ID", example = "2")
            Long id,
            @Schema(description = "멤버 이름", example = "김철수")
            String name,
            @Schema(description = "멤버 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
            String profileImage,
            @Schema(description = "마지막 리마인드 발송 시각", example = "2026-08-27T09:00:00", nullable = true)
            LocalDateTime lastRemindAt
    ) {
        public static IncompleteMember of(Long studyMemberId, String name, String profileImageUrl,
                                          LocalDateTime lastRemindAt) {
            return new IncompleteMember(studyMemberId, name, profileImageUrl, lastRemindAt);
        }
    }
}
