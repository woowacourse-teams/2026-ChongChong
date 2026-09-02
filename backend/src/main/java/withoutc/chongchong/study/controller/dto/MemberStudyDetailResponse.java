package withoutc.chongchong.study.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MemberStudyDetailResponse(
        @Schema(description = "미확인 공지·미제출 과제 전체 수", example = "2")
        int totalCount,
        @Schema(description = "확인이 필요한 공지 요약")
        MemberNoticeSummaryListResponse notices,
        @Schema(description = "제출이 필요한 과제 요약")
        MemberAssignmentSummaryListResponse assignments
) implements StudyDetailResponse {

    public record MemberNoticeSummaryListResponse(
            @Schema(description = "공지 요약 목록")
            List<MemberNoticeSummaryResponse> items
    ) {

        public static MemberNoticeSummaryListResponse from(List<MemberNoticeSummaryResponse> noticeResponses) {
            return new MemberNoticeSummaryListResponse(noticeResponses);
        }
    }

    public record MemberNoticeSummaryResponse(
            @Schema(description = "공지 ID", example = "1")
            Long id,
            @Schema(description = "공지 제목", example = "이번 주 공지")
            String title
    ) {
    }

    public record MemberAssignmentSummaryListResponse(
            @Schema(description = "과제 요약 목록")
            List<MemberAssignmentSummaryResponse> items
    ) {

        public static MemberAssignmentSummaryListResponse from(
                List<MemberAssignmentSummaryResponse> assignmentResponses) {
            return new MemberAssignmentSummaryListResponse(assignmentResponses);
        }
    }

    public record MemberAssignmentSummaryResponse(
            @Schema(description = "과제 ID", example = "1")
            Long id,
            @Schema(description = "과제 제목", example = "1주 차 과제")
            String title
    ) {
    }
}
