package withoutc.chongchong.assignment.support;

import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.study.entity.Study;

public final class AssignmentTestFixture {

    private AssignmentTestFixture() {
    }

    public static Assignment assignmentWithId(Long assignmentId, Long studyId, LocalDateTime now) {
        Study study = Study.create("자바 스터디", "설명");
        ReflectionTestUtils.setField(study, "id", studyId);
        Assignment assignment = Assignment.create(
                study, "과제 제목", "과제 내용", "링크 제출", now.plusDays(7), now
        );
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        return assignment;
    }
}
