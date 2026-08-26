package withoutc.chongchong.assignment.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentStatusesResponse(
        Long id,
        int memberCount,
        int completeCount,
        int incompleteCount,
        LocalDateTime remindAt,
        List<CompleteMember> completeMembers,
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
            Long id,
            String name,
            String profileImage
    ) {
        public static CompleteMember of(Long studyMemberId, String name, String profileImageUrl) {
            return new CompleteMember(studyMemberId, name, profileImageUrl);
        }

    }

    public record IncompleteMember(
            Long id,
            String name,
            String profileImage,
            LocalDateTime lastRemindAt
    ) {
        public static IncompleteMember of(Long studyMemberId, String name, String profileImageUrl,
                                          LocalDateTime lastRemindAt) {
            return new IncompleteMember(studyMemberId, name, profileImageUrl, lastRemindAt);
        }
    }
}
