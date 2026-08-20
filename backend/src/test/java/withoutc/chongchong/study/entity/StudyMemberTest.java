package withoutc.chongchong.study.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.user.entity.User;

class StudyMemberTest {

    private final Study study = mock(Study.class);
    private final User user = mock(User.class);

    @Test
    @DisplayName("스터디 리더의 역할을 확인한다")
    void isLeaderTest() {
        StudyMember member = createMember(StudyMemberRole.LEADER);

        assertThat(member.isLeader()).isTrue();
    }

    @Test
    @DisplayName("일반 스터디원은 리더가 아님을 확인한다")
    void isNotLeaderTest() {
        StudyMember member = createMember(StudyMemberRole.MEMBER);

        assertThat(member.isLeader()).isFalse();
    }

    private StudyMember createMember(StudyMemberRole role) {
        return StudyMember.create(study, user, "스터디원", "https://example.com/profile.png", role);
    }
}
