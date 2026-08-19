package withoutc.chongchong.notice.dto;

import java.util.List;

public record NoticeListResponse(
        Long nextCursor,
        boolean hasNext,
        List<NoticeSummaryResponse> notices
) {
    public static NoticeListResponse of(
            Long nextCursor,
            boolean hasNext,
            List<NoticeSummaryResponse> notices
    ) {
        return new NoticeListResponse(nextCursor, hasNext, notices);
    }
}
