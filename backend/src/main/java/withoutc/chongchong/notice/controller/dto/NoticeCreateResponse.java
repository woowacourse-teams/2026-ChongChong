package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import withoutc.chongchong.notice.entity.Notice;

public record NoticeCreateResponse(
        @Schema(description = "생성된 공지 ID", example = "1")
        Long noticeId
) {
    public static NoticeCreateResponse from(Notice notice) {
        return new NoticeCreateResponse(notice.getId());
    }
}
