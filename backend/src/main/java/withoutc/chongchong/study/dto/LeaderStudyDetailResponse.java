package withoutc.chongchong.study.dto;

import java.util.List;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.notice.entity.Notice;

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
            long id,
            String title,
            int completeCount
    ) {

        public static LeaderNoticeSummaryResponse from(Notice notice, int completeCount) {
            return new LeaderNoticeSummaryResponse(
                    notice.getId(),
                    notice.getTitle(),
                    completeCount
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
            long id,
            String title,
            int completeCount
    ) {

        public static LeaderAssignmentSummaryResponse from(Assignment assignment, int completeCount) {
            return new LeaderAssignmentSummaryResponse(
                    assignment.getId(),
                    assignment.getTitle(),
                    completeCount
            );
        }
    }
}
