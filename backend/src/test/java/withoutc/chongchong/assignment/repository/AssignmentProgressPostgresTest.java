package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.projection.AssignmentProgressProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class AssignmentProgressPostgresTest extends PostgresContainerTest {

    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository memberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("집계 쿼리는 제출 대상 제외와 발송된 리마인더를 엔티티 계산과 같게 반영한다")
    void progressMatchesEntityCalculation() {
        LocalDateTime now = LocalDateTime.now();
        Long assignmentId = new TransactionTemplate(transactionManager).execute(status -> {
            Study study = studyRepository.save(Study.create("집계 검증", "설명", false));
            List<StudyMember> members = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                User user = userRepository.save(User.create("멤버" + i, null));
                members.add(memberRepository.save(
                        StudyMember.create(study, user, "멤버" + i, null, StudyMemberRole.MEMBER)));
            }
            Assignment assignment = Assignment.create(study, "과제", "내용", null,
                    SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY, now.plusDays(10), now, members);
            assignment.addReminders(List.of(now.plusDays(1), now.plusDays(2), now.plusDays(3)), now);
            assignment.getReminders().getFirst().markAsSent();
            assignment.getSubmissions().get(0).submit("제출", null, now);
            assignment.getSubmissions().get(1).submit("제출", null, now);
            assignment.getSubmissions().get(1).unassign();
            return assignmentRepository.save(assignment).getId();
        });

        AssignmentProgressProjection progress = assignmentRepository.findProgressByIdIn(List.of(assignmentId))
                .getFirst();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
            assertThat(progress.memberCount()).isEqualTo(assignment.getSubmissionCount()).isEqualTo(3);
            assertThat(progress.completeCount()).isEqualTo(assignment.getSubmittedCount(now)).isEqualTo(1);
            assertThat(progress.nextRemindAt()).isEqualTo(assignment.getNextRemindAt());
        });
    }
}
