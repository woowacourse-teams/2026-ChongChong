package withoutc.chongchong.assignment.support;

import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.user.entity.User;

public final class AssignmentTestFixture {

    private AssignmentTestFixture() {
    }

    public static Assignment assignmentWithId(Long assignmentId, Long studyId, LocalDateTime now) {
        Study study = Study.create("자바 스터디", "설명");
        User user = User.create("리더", null);
        StudyMember writer = StudyMember.create(study, user, "리더", null, StudyMemberRole.LEADER);
        ReflectionTestUtils.setField(study, "id", studyId);
        Assignment assignment = Assignment.create(
                writer, "과제 제목", "과제 내용", "링크 제출", now.plusDays(7), now
        );
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        return assignment;
    }
}
