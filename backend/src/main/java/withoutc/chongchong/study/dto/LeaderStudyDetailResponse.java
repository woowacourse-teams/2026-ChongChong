package withoutc.chongchong.study.dto;

import java.util.List;
import withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection;
import withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection;

public record LeaderStudyDetailResponse(
        int memberCount,
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
            int completeCount
    ) {

        public static LeaderNoticeSummaryResponse from(LeaderNoticeSummaryProjection noticeProjection) {
            return new LeaderNoticeSummaryResponse(
                    noticeProjection.id(),
                    noticeProjection.title(),
                    (int) noticeProjection.completeCount()
            );
        }
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
            int completeCount
    ) {

        public static LeaderAssignmentSummaryResponse from(LeaderAssignmentSummaryProjection assignmentProjection) {
            return new LeaderAssignmentSummaryResponse(
                    assignmentProjection.id(),
                    assignmentProjection.title(),
                    (int) assignmentProjection.completeCount()
            );
        }
    }
}
