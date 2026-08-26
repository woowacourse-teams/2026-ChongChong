package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class AssignmentRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 9, 0);

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("스터디별 과제를 최신순으로 조회하고 cursor보다 작은 id부터 다음 페이지를 조회한다")
    void findByCursorTest() {
        StudyFixture firstStudy = createStudyFixture("첫 번째 스터디", "첫 번째 리더");
        StudyFixture secondStudy = createStudyFixture("두 번째 스터디", "두 번째 리더");
        List<Assignment> assignments = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            assignments.add(assignmentRepository.save(
                    Assignment.create(
                            firstStudy.leader(),
                            "과제 " + index,
                            "과제 내용 " + index,
                            "GitHub PR",
                            LocalDateTime.of(2026, 8, 30, 23, 59),
                            NOW
                    )
            ));
        }
        assignmentRepository.save(Assignment.create(
                secondStudy.leader(),
                "다른 과제",
                "다른 과제 내용",
                "GitHub PR",
                LocalDateTime.of(2026, 8, 30, 23, 59),
                NOW
        ));
        assignmentRepository.flush();

        List<Assignment> firstPage = assignmentRepository.findByCursor(
                firstStudy.study().getId(),
                null,
                PageRequest.of(0, 3)
        );
        List<Assignment> secondPage = assignmentRepository.findByCursor(
                firstStudy.study().getId(),
                firstPage.getLast().getId(),
                PageRequest.of(0, 3)
        );

        assertThat(firstPage)
                .extracting(Assignment::getId)
                .containsExactly(assignments.get(4).getId(), assignments.get(3).getId(), assignments.get(2).getId());
        assertThat(secondPage)
                .extracting(Assignment::getId)
                .containsExactly(assignments.get(1).getId(), assignments.getFirst().getId());
    }

    @Test
    @DisplayName("과제 조회에 성공하면 해당 과제를 반환하고, 없으면 과제 없음 예외를 던진다")
    void getByIdOrThrowTest() {
        StudyFixture fixture = createStudyFixture("스터디", "리더");
        Assignment assignment = assignmentRepository.save(Assignment.create(
                fixture.leader(),
                "과제",
                "과제 내용",
                "GitHub PR",
                LocalDateTime.of(2026, 8, 30, 23, 59),
                NOW
        ));

        Assignment found = assignmentRepository.getByIdOrThrow(assignment.getId());

        assertThat(found.getId()).isEqualTo(assignment.getId());
        assertThatThrownBy(() -> assignmentRepository.getByIdOrThrow(Long.MAX_VALUE))
                .isInstanceOfSatisfying(AssignmentException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND)
                );
    }

    private StudyFixture createStudyFixture(String studyName, String userName) {
        User user = userRepository.save(User.create(userName, null));
        Study study = studyRepository.save(Study.create(studyName, "설명"));
        StudyMember leader = studyMemberRepository.save(
                StudyMember.create(study, user, userName, null, StudyMemberRole.LEADER)
        );
        return new StudyFixture(study, leader);
    }

    private record StudyFixture(Study study, StudyMember leader) {
    }
}
