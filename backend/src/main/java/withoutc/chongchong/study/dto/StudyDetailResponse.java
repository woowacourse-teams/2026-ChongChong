package withoutc.chongchong.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "스터디 상세 응답",
        oneOf = {LeaderStudyDetailResponse.class, MemberStudyDetailResponse.class}
)
public interface StudyDetailResponse {
}
