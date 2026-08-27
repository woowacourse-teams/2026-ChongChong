package withoutc.chongchong.notice.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.Notice;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공지 요약 응답. 리더와 멤버에 따라 일부 필드가 다르게 제공된다.")
public record NoticeSummaryResponse(
        @Schema(description = "공지 ID", example = "1")
        Long id,
        @Schema(description = "공지 제목", example = "이번 주 공지")
        String title,
        @Schema(description = "공지 내용", example = "이번 주 스터디 일정을 확인해주세요.")
        String content,
        @Schema(description = "공지 생성 시각", example = "2026-08-27T10:00:00")
        LocalDateTime createdAt,
        @Schema(description = "공지 수신 멤버 수. 멤버 응답에서는 제공되지 않는다.", example = "5", nullable = true)
        Integer recipientCount,
        @Schema(description = "공지 읽음 완료 멤버 수. 멤버 응답에서는 제공되지 않는다.", example = "3", nullable = true)
        Integer readRecipientCount,
        @Schema(description = "다음 리마인드 예정 시각. 멤버 응답에서는 제공되지 않는다.", example = "2026-08-28T10:00:00", nullable = true)
        LocalDateTime remindAt,
        @Schema(description = "공지 확인 완료 여부", example = "false")
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
                notice.getNextRemindAt(),
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
