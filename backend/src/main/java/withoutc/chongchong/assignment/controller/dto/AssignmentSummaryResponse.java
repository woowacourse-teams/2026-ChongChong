package withoutc.chongchong.assignment.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.assignment.entity.Assignment;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "과제 요약 응답. 리더와 멤버에 따라 일부 필드가 다르게 제공된다.")
public record AssignmentSummaryResponse(
        @Schema(description = "과제 ID", example = "1")
        Long id,
        @Schema(description = "과제 제목", example = "1주 차 과제")
        String title,
        @Schema(description = "과제 내용", example = "이번 주 과제를 제출해주세요.")
        String content,
        @Schema(description = "과제 제출 방법", example = "링크 제출")
        String submissionMethod,
        @Schema(description = "과제 마감 시각", example = "2026-08-29T23:59:00")
        LocalDateTime closeAt,
        @Schema(description = "과제 대상 멤버 수. 멤버 응답에서는 제공되지 않는다.", example = "5", nullable = true)
        Integer memberCount,
        @Schema(description = "과제 제출 완료 멤버 수. 멤버 응답에서는 제공되지 않는다.", example = "3", nullable = true)
        Integer completeCount,
        @Schema(description = "다음 리마인드 예정 시각. 멤버 응답에서는 제공되지 않는다.", example = "2026-08-28T10:00:00", nullable = true)
        LocalDateTime remindAt,
        @Schema(description = "과제 제출 완료 여부", example = "false")
        boolean isComplete
) {
    public static AssignmentSummaryResponse forLeader(Assignment assignment) {

        int memberCount = assignment.getSubmissionCount();
        int completeCount = assignment.getSubmittedCount();

        boolean isComplete = (memberCount == completeCount);

        return new AssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getSubmissionMethod(),
                assignment.getCloseAt(),
                memberCount,
                completeCount,
                assignment.getNextRemindAt(),
                isComplete
        );
    }

    public static AssignmentSummaryResponse forMember(Assignment assignment, boolean isComplete) {
        return new AssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getSubmissionMethod(),
                assignment.getCloseAt(),
                null,
                null,
                null,
                isComplete
        );
    }
}
