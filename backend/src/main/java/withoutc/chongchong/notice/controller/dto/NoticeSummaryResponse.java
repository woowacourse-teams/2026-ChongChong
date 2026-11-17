package withoutc.chongchong.notice.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReadStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoticeSummaryResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        Integer recipientCount,
        Integer readRecipientCount,
        LocalDateTime remindAt,
        NoticeReadStatus readStatus,
        Boolean isComplete
) {
    public static NoticeSummaryResponse forLeader(Notice notice, int recipientCount, int readRecipientCount,
                                                  LocalDateTime nextRemindAt) {
        boolean isComplete = (recipientCount == readRecipientCount);

        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                recipientCount,
                readRecipientCount,
                nextRemindAt,
                null,
                isComplete
        );
    }

    public static NoticeSummaryResponse forMember(Notice notice, NoticeReadStatus readStatus) {
        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                null,
                null,
                null,
                readStatus,
                null
        );
    }
}
