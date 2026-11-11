package withoutc.chongchong.study.repository.projection;

import withoutc.chongchong.study.entity.StudyMember;

public record StudyMemberIdentityProjection(Long memberId, Long userId) {

    public boolean matches(StudyMember member) {
        return memberId.equals(member.getId()) && userId.equals(member.getUser().getId());
    }
}
