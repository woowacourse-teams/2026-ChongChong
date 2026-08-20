package withoutc.chongchong.notice.dto;

public record NoticeCreateResponse(
        Long noticeId
) {
    public static NoticeCreateResponse of(Long noticeId) {
        return new NoticeCreateResponse(noticeId);
    }
}
