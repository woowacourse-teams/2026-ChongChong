package withoutc.chongchong.notice.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.user.entity.User;

class NoticeAccessPolicyTest {

    private final NoticeAccessPolicy policy = new NoticeAccessPolicy();

    @ParameterizedTest(name = "{0}: 리더는 허용한다")
    @MethodSource("leaderOnlyActions")
    void allowLeaderTest(String name, BiConsumer<NoticeAccessPolicy, StudyMember> action) {
        StudyMember leader = member(StudyMemberRole.LEADER);

        assertThatCode(() -> action.accept(policy, leader)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}: 일반 멤버는 거부한다")
    @MethodSource("leaderOnlyActions")
    void rejectMemberTest(String name, BiConsumer<NoticeAccessPolicy, StudyMember> action) {
        StudyMember member = member(StudyMemberRole.MEMBER);

        assertThatThrownBy(() -> action.accept(policy, member))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);
    }

    private static Stream<Arguments> leaderOnlyActions() {
        return Stream.of(
                arguments("공지 생성", (BiConsumer<NoticeAccessPolicy, StudyMember>)
                        NoticeAccessPolicy::requireCanCreateNotice),
                arguments("공지 수정", (BiConsumer<NoticeAccessPolicy, StudyMember>)
                        NoticeAccessPolicy::requireCanUpdateNotice),
                arguments("공지 삭제", (BiConsumer<NoticeAccessPolicy, StudyMember>)
                        NoticeAccessPolicy::requireCanDeleteNotice),
                arguments("공지 읽음 현황 조회", (BiConsumer<NoticeAccessPolicy, StudyMember>)
                        NoticeAccessPolicy::requireCanReadNoticeReadStatuses)
        );
    }

    private StudyMember member(StudyMemberRole role) {
        Study study = Study.create("스터디", "설명");
        User user = User.create("사용자", null);
        return StudyMember.create(study, user, "사용자", null, role);
    }
}
