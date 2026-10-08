package withoutc.chongchong.notice.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeReadStatusesResponse(
        Long id,
        int memberCount,
        int readCount,
        int unreadCount,
        LocalDateTime remindAt,
        List<ReadMember> readMembers,
        List<UnreadMember> unreadMembers
) {
    public static NoticeReadStatusesResponse of(
            Long noticeId,
            LocalDateTime remindAt,
            List<ReadMember> readMembers,
            List<UnreadMember> unreadMembers
    ) {
        return new NoticeReadStatusesResponse(
                noticeId,
                readMembers.size() + unreadMembers.size(),
                readMembers.size(),
                unreadMembers.size(),
                remindAt,
                readMembers,
                unreadMembers
        );
    }


    public record ReadMember(
            Long id,
            String name,
            String profileImage,
            LocalDateTime readAt
    ) {
        public static ReadMember of(Long studyMemberId, String name, String profileImageUrl,
                                    LocalDateTime readAt) {
            return new ReadMember(studyMemberId, name, profileImageUrl, readAt);
        }

    }

    public record UnreadMember(
            Long id,
            String name,
            String profileImage,
            LocalDateTime lastRemindAt
    ) {
        public static UnreadMember of(Long studyMemberId, String name, String profileImageUrl,
                                      LocalDateTime lastRemindAt) {
            return new UnreadMember(studyMemberId, name, profileImageUrl, lastRemindAt);
        }
    }
}
