package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NoticeListResponse(
        @Schema(description = "다음 페이지 조회에 사용할 커서", example = "10", nullable = true)
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext,
        @Schema(description = "공지 요약 목록")
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
