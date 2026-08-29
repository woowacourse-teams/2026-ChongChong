package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeStatusesResponse(
        @Schema(description = "공지 ID", example = "1")
        Long id,
        @Schema(description = "공지 수신 멤버 수", example = "5")
        int memberCount,
        @Schema(description = "공지 읽음 완료 멤버 수", example = "3")
        int readCount,
        @Schema(description = "다음 리마인드 예정 시각", example = "2026-08-28T10:00:00", nullable = true)
        LocalDateTime remindAt,
        @Schema(description = "읽은 멤버 목록")
        List<ReadMember> readMembers,
        @Schema(description = "읽지 않은 멤버 목록")
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
            @Schema(description = "스터디 멤버 ID", example = "1")
            Long id,
            @Schema(description = "멤버 이름", example = "홍길동")
            String name,
            @Schema(description = "멤버 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
            String profileImageUrl
    ) {
        public static ReadMember of(Long studyMemberId, String name, String profileImageUrl) {
            return new ReadMember(studyMemberId, name, profileImageUrl);
        }

    }

    public record UnreadMember(
            @Schema(description = "스터디 멤버 ID", example = "2")
            Long id,
            @Schema(description = "멤버 이름", example = "김철수")
            String name,
            @Schema(description = "멤버 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
            String profileImageUrl,
            @Schema(description = "마지막 리마인드 발송 시각", example = "2026-08-27T09:00:00", nullable = true)
            LocalDateTime lastRemindAt
    ) {
        public static UnreadMember of(Long studyMemberId, String name, String profileImageUrl,
                                      LocalDateTime lastRemindAt) {
            return new UnreadMember(studyMemberId, name, profileImageUrl, lastRemindAt);
        }
    }
}
