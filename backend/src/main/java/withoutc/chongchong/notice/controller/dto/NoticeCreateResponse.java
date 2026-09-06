package withoutc.chongchong.notice.controller.dto;

import withoutc.chongchong.notice.entity.Notice;

public record NoticeCreateResponse(
        Long noticeId
) {
    public static NoticeCreateResponse from(Notice notice) {
        return new NoticeCreateResponse(notice.getId());
    }
}
