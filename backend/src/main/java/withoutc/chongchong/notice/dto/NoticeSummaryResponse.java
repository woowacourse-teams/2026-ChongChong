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
        Integer recipientCount,
        Integer readRecipientCount,
        LocalDateTime remindAt,
        boolean isComplete
) {
    public static NoticeSummaryResponse forLeader(Notice notice) {

        int recipientCount = notice.getRecipientCount();
        int readRecipientCount = notice.getReadRecipientCount();

        boolean isComplete = (recipientCount == readRecipientCount);

        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                recipientCount,
                readRecipientCount,
                notice.getLatestRemindAt(),
                isComplete
        );
    }

    public static NoticeSummaryResponse forMember(Notice notice, boolean isComplete) {
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
