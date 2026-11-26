package withoutc.chongchong.study.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.user.entity.User;

class StudyAccessPolicyTest {

    private final StudyAccessPolicy policy = new StudyAccessPolicy();

    @ParameterizedTest(name = "{0}: 리더는 허용한다")
    @MethodSource("leaderOnlyActions")
    void allowLeaderTest(String name, BiConsumer<StudyAccessPolicy, StudyMember> action) {
        StudyMember leader = member(StudyMemberRole.LEADER);

        assertThatCode(() -> action.accept(policy, leader)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}: 일반 멤버는 거부한다")
    @MethodSource("leaderOnlyActions")
    void rejectMemberTest(String name, BiConsumer<StudyAccessPolicy, StudyMember> action) {
        StudyMember member = member(StudyMemberRole.MEMBER);

        assertThatThrownBy(() -> action.accept(policy, member))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.NOT_STUDY_LEADER);
    }

    private static Stream<Arguments> leaderOnlyActions() {
        return Stream.of(
                arguments("스터디 수정", (BiConsumer<StudyAccessPolicy, StudyMember>)
                        StudyAccessPolicy::requireCanUpdateStudy),
                arguments("스터디 삭제", (BiConsumer<StudyAccessPolicy, StudyMember>)
                        StudyAccessPolicy::requireCanDeleteStudy),
                arguments("스터디원 방출", (BiConsumer<StudyAccessPolicy, StudyMember>)
                        StudyAccessPolicy::requireCanExpelMember)
        );
    }

    private StudyMember member(StudyMemberRole role) {
        Study study = Study.create("스터디", "설명", false);
        User user = User.create("사용자", null);
        return StudyMember.create(study, user, "사용자", null, role);
    }
}
