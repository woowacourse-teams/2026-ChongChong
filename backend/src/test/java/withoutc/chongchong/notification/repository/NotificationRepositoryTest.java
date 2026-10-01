package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
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
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Test
    @DisplayName("사용자가 받은 알림만 모두 삭제한다")
    void deleteAllByRecipientIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember target = createMember(study, "삭제 대상");
        StudyMember otherMember = createMember(study, "다른 스터디원");
        saveNotification(study, target, 1L);
        saveNotification(study, otherMember, 1L);

        int deletedCount = notificationRepository.deleteAllByRecipientId(target.getUser().getId());

        assertThat(deletedCount).isOne();
        assertThat(notificationRepository.findAll())
                .extracting(notification -> notification.getRecipient().getId())
                .containsExactly(otherMember.getUser().getId());
    }

    @Test
    @DisplayName("스터디원에게 속한 제출물 알림만 모두 삭제한다")
    void deleteAllByMemberIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember target = createMember(study, "삭제 대상", StudyMemberRole.MEMBER);
        StudyMember otherMember = createMember(study, "다른 스터디원", StudyMemberRole.MEMBER);
        Assignment assignment = Assignment.create(
                study,
                "과제",
                "내용",
                "링크",
                SubmissionTarget.MEMBERS_ONLY,
                LocalDateTime.of(2030, 1, 2, 0, 0),
                LocalDateTime.of(2030, 1, 1, 0, 0)
        );
        assignment.initializeSubmissions(List.of(target, otherMember));
        assignmentRepository.saveAndFlush(assignment);

        AssignmentSubmission targetSubmission = assignmentSubmissionRepository
                .findByAssignmentIdAndMemberId(assignment.getId(), target.getId())
                .orElseThrow();
        AssignmentSubmission otherSubmission = assignmentSubmissionRepository
                .findByAssignmentIdAndMemberId(assignment.getId(), otherMember.getId())
                .orElseThrow();
        Notification targetSubmissionNotification = saveSubmissionNotification(
                study, assignment, leader, targetSubmission.getId());
        Notification otherSubmissionNotification = saveSubmissionNotification(
                study, assignment, leader, otherSubmission.getId());
        Notification targetAssignmentNotification = saveAssignmentNotification(study, target, assignment.getId());

        notificationRepository.deleteAllByMemberId(target.getId());

        assertThat(notificationRepository.existsById(targetSubmissionNotification.getId())).isFalse();
        assertThat(notificationRepository.existsById(otherSubmissionNotification.getId())).isTrue();
        assertThat(notificationRepository.existsById(targetAssignmentNotification.getId())).isTrue();
    }

    private StudyMember createMember(Study study, String name) {
        return createMember(study, name, StudyMemberRole.MEMBER);
    }

    private StudyMember createMember(Study study, String name, StudyMemberRole role) {
        User user = userRepository.save(User.create(name, null));
        return studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, name, null, role)
        );
    }

    private Notification saveNotification(Study study, StudyMember recipient, Long resourceId) {
        Notification notification = Notification.create(
                recipient.getUser(),
                "[스터디] 새 공지",
                "공지 제목",
                NotificationType.REMIND,
                resourceId,
                ResourceType.NOTICE,
                "/studies/%d/notices/%d".formatted(study.getId(), resourceId)
        );
        return notificationRepository.saveAndFlush(notification);
    }

    private Notification saveSubmissionNotification(
            Study study, Assignment assignment, StudyMember recipient, Long submissionId) {
        Notification notification = Notification.create(
                recipient.getUser(),
                "[스터디] 제출물 알림",
                "제출물이 등록되었습니다",
                NotificationType.REMIND,
                submissionId,
                ResourceType.ASSIGNMENT_SUBMISSION,
                "/studies/%d/assignments/%d/submissions/%d".formatted(
                        study.getId(), assignment.getId(), submissionId)
        );
        return notificationRepository.saveAndFlush(notification);
    }

    private Notification saveAssignmentNotification(Study study, StudyMember recipient, Long assignmentId) {
        Notification notification = Notification.create(
                recipient.getUser(),
                "[스터디] 새 과제",
                "과제가 등록되었습니다",
                NotificationType.REMIND,
                assignmentId,
                ResourceType.ASSIGNMENT,
                "/studies/%d/assignments/%d".formatted(study.getId(), assignmentId)
        );
        return notificationRepository.saveAndFlush(notification);
    }
}
