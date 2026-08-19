package withoutc.chongchong.notice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.Notice;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoticeSummaryResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        Integer totalMemberCount,
        Integer readMemberCount,
        LocalDateTime remindAt,
        boolean isComplete
) {
    public static NoticeSummaryResponse toLeader(Notice notice) {

        int memberCount = notice.getRecipientsCount();
        int completeCount = notice.getReadCount();

        boolean isComplete = (memberCount == completeCount);

        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                memberCount,
                completeCount,
                notice.getLastRemindAt(),
                isComplete
        );
    }

    public static NoticeSummaryResponse toMember(Notice notice, boolean isComplete) {
        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                null,
                null,
                null,
                isComplete
        );
    }
}
