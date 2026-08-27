package withoutc.chongchong.notice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.Notice;

public record NoticeDetailResponse(
        @Schema(description = "공지 ID", example = "1")
        Long id,
        @Schema(description = "공지 제목", example = "이번 주 공지")
        String title,
        @Schema(description = "공지 작성자", example = "홍길동")
        String writer,
        @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
        String profileImageUrl,
        @Schema(description = "공지 내용", example = "이번 주 스터디 일정을 확인해주세요.")
        String content,
        @Schema(description = "공지 생성 시각", example = "2026-08-27T10:00:00")
        LocalDateTime createdAt
) {
    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getWriter().getName(),
                notice.getWriter().getProfileImageUrl(),
                notice.getContent(),
                notice.getCreatedAt()
        );
    }
}
