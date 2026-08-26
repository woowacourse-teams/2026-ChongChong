package withoutc.chongchong.notice.controller.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.notice.entity.Notice;

public record NoticeDetailResponse(
        Long id,
        String title,
        String writer,
        String profileImage,
        String content,
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
