package withoutc.chongchong.notice.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.NoticeReadStatus;
import withoutc.chongchong.notice.entity.NoticeRecipient;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoticeReadStatusResponse(
        NoticeReadStatus readStatus,
        LocalDateTime readAt
) {
    public static NoticeReadStatusResponse notAssigned() {
        return new NoticeReadStatusResponse(NoticeReadStatus.NOT_ASSIGNED, null);
    }

    public static NoticeReadStatusResponse from(NoticeRecipient noticeRecipient) {
        return new NoticeReadStatusResponse(noticeRecipient.readStatus(), noticeRecipient.getReadAt());
    }
}
