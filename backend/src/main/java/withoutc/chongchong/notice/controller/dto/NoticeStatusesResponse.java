package withoutc.chongchong.notice.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeStatusesResponse(
        Long id,
        int memberCount,
        int readCount,
        LocalDateTime remindAt,
        List<ReadMember> readMembers,
        List<UnreadMember> unreadMembers
) {
    public static NoticeStatusesResponse of(
            Long noticeId,
            LocalDateTime remindAt,
            List<ReadMember> readMembers,
            List<UnreadMember> unreadMembers
    ) {
        return new NoticeStatusesResponse(
                noticeId,
                readMembers.size() + unreadMembers.size(),
                readMembers.size(),
                remindAt,
                readMembers,
                unreadMembers
        );
    }


    public record ReadMember(
            Long id,
            String name,
            String profileImageUrl
    ) {
        public static ReadMember of(Long studyMemberId, String name, String profileImageUrl) {
            return new ReadMember(studyMemberId, name, profileImageUrl);
        }

    }

    public record UnreadMember(
            Long id,
            String name,
            String profileImageUrl,
            LocalDateTime lastRemindAt
    ) {
        public static UnreadMember of(Long studyMemberId, String name, String profileImageUrl,
                                      LocalDateTime lastRemindAt) {
            return new UnreadMember(studyMemberId, name, profileImageUrl, lastRemindAt);
        }
    }
}
