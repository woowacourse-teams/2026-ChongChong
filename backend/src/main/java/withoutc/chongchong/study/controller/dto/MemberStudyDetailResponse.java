package withoutc.chongchong.study.controller.dto;

import java.util.List;

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
            Long id,
            String title
    ) {
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
            Long id,
            String title
    ) {
    }
}
