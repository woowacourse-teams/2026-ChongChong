package withoutc.chongchong.study.dto;

import java.util.List;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.notice.entity.Notice;

public record MemberStudyDetailResponse(
        int totalCount,
        MemberNoticeSummaryListResponse notices,
        MemberAssignmentSummaryListResponse assignments
) implements StudyDetailResponse {

    public record MemberNoticeSummaryListResponse(
            List<MemberNoticeSummaryResponse> items
    ) {

        public static MemberNoticeSummaryListResponse from(List<MemberNoticeSummaryResponse> noticeResponses) {
            return new MemberNoticeSummaryListResponse(noticeResponses);
        }
    }

    public record MemberNoticeSummaryResponse(
            long id,
            String title
    ) {

        public static MemberNoticeSummaryResponse from(Notice notice) {
            return new MemberNoticeSummaryResponse(
                    notice.getId(),
                    notice.getTitle()
            );
        }
    }

    public record MemberAssignmentSummaryListResponse(
            List<MemberAssignmentSummaryResponse> items
    ) {

        public static MemberAssignmentSummaryListResponse from(
                List<MemberAssignmentSummaryResponse> assignmentResponses) {
            return new MemberAssignmentSummaryListResponse(assignmentResponses);
        }
    }

    public record MemberAssignmentSummaryResponse(
            long id,
            String title
    ) {

        public static MemberAssignmentSummaryResponse from(Assignment assignment) {
            return new MemberAssignmentSummaryResponse(
                    assignment.getId(),
                    assignment.getTitle()
            );
        }
    }
}
