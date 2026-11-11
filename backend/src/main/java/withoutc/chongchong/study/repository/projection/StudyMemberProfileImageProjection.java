package withoutc.chongchong.study.repository.projection;

public record StudyMemberProfileImageProjection(
        Long memberId,
        String externalUrl,
        String objectKey,
        String contentType
) {
}
