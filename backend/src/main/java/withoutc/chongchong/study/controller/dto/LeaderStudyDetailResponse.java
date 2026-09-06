package withoutc.chongchong.study.controller.dto;

import java.util.List;

public record LeaderStudyDetailResponse(
        LeaderNoticeSummaryListResponse notices,
        LeaderAssignmentSummaryListResponse assignments
) implements StudyDetailResponse {

    public record LeaderNoticeSummaryListResponse(
            int count,
            List<LeaderNoticeSummaryResponse> items
    ) {

        public static LeaderNoticeSummaryListResponse from(List<LeaderNoticeSummaryResponse> noticeResponses) {
            return new LeaderNoticeSummaryListResponse(noticeResponses.size(), noticeResponses);
        }
    }

    public record LeaderNoticeSummaryResponse(
            Long id,
            String title,
            int memberCount,
            int completeCount
    ) {
    }

    public record LeaderAssignmentSummaryListResponse(
            int count,
            List<LeaderAssignmentSummaryResponse> items
    ) {

        public static LeaderAssignmentSummaryListResponse from(
                List<LeaderAssignmentSummaryResponse> assignmentResponses) {
            return new LeaderAssignmentSummaryListResponse(assignmentResponses.size(), assignmentResponses);
        }
    }

    public record LeaderAssignmentSummaryResponse(
            Long id,
            String title,
            int memberCount,
            int completeCount
    ) {
    }
}
