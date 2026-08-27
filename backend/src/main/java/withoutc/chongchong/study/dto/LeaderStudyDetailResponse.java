package withoutc.chongchong.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection;
import withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection;

public record LeaderStudyDetailResponse(
        @Schema(description = "리더가 확인할 공지 요약")
        LeaderNoticeSummaryListResponse notices,
        @Schema(description = "리더가 확인할 과제 요약")
        LeaderAssignmentSummaryListResponse assignments
) implements StudyDetailResponse {

    public record LeaderNoticeSummaryListResponse(
            @Schema(description = "공지 수", example = "3")
            int count,
            @Schema(description = "공지 요약 목록")
            List<LeaderNoticeSummaryResponse> items
    ) {

        public static LeaderNoticeSummaryListResponse from(List<LeaderNoticeSummaryResponse> noticeResponses) {
            return new LeaderNoticeSummaryListResponse(noticeResponses.size(), noticeResponses);
        }
    }

    public record LeaderNoticeSummaryResponse(
            @Schema(description = "공지 ID", example = "1")
            Long id,
            @Schema(description = "공지 제목", example = "이번 주 공지")
            String title,
            @Schema(description = "공지 대상 멤버 수", example = "5")
            int memberCount,
            @Schema(description = "공지 확인 완료 멤버 수", example = "3")
            int completeCount
    ) {

        public static LeaderNoticeSummaryResponse from(LeaderNoticeSummaryProjection noticeProjection) {
            return new LeaderNoticeSummaryResponse(
                    noticeProjection.id(),
                    noticeProjection.title(),
                    (int) noticeProjection.memberCount(),
                    (int) noticeProjection.completeCount()
            );
        }
    }

    public record LeaderAssignmentSummaryListResponse(
            @Schema(description = "과제 수", example = "4")
            int count,
            @Schema(description = "과제 요약 목록")
            List<LeaderAssignmentSummaryResponse> items
    ) {

        public static LeaderAssignmentSummaryListResponse from(
                List<LeaderAssignmentSummaryResponse> assignmentResponses) {
            return new LeaderAssignmentSummaryListResponse(assignmentResponses.size(), assignmentResponses);
        }
    }

    public record LeaderAssignmentSummaryResponse(
            @Schema(description = "과제 ID", example = "1")
            Long id,
            @Schema(description = "과제 제목", example = "1주 차 과제")
            String title,
            @Schema(description = "과제 대상 멤버 수", example = "5")
            int memberCount,
            @Schema(description = "과제 제출 완료 멤버 수", example = "3")
            int completeCount
    ) {

        public static LeaderAssignmentSummaryResponse from(LeaderAssignmentSummaryProjection assignmentProjection) {
            return new LeaderAssignmentSummaryResponse(
                    assignmentProjection.id(),
                    assignmentProjection.title(),
                    (int) assignmentProjection.memberCount(),
                    (int) assignmentProjection.completeCount()
            );
        }
    }
}
