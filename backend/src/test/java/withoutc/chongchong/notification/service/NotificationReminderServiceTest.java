package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.assignment.entity.AssignmentReminder;
import withoutc.chongchong.assignment.repository.AssignmentReminderRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeReminderRepository;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.projection.ReminderCandidateProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class NotificationReminderServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"),
            ZoneId.of("Asia/Seoul"));

    @Mock
    private NoticeReminderRepository noticeReminderRepository;
    @Mock
    private AssignmentReminderRepository assignmentReminderRepository;
    @Mock
    private NoticeRecipientRepository noticeRecipientRepository;
    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private StudyMemberRepository studyMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private NotificationReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new NotificationReminderService(noticeReminderRepository, assignmentReminderRepository,
                noticeRecipientRepository, assignmentSubmissionRepository, noticeRepository, assignmentRepository,
                studyMemberRepository, userRepository,
                notificationService, CLOCK);
    }

    @Test
    @DisplayName("공지와 과제의 기한이 지난 후보를 잠금 없이 정해진 개수로 탐색한다")
    void findCandidates() {
        ReminderCandidateProjection notice = new ReminderCandidateProjection(ResourceType.NOTICE, 1L, 10L, 3L);
        ReminderCandidateProjection assignment = new ReminderCandidateProjection(ResourceType.ASSIGNMENT, 2L, 20L, 3L);
        when(noticeReminderRepository.findDueCandidates(NOW, PageRequest.of(0, 100))).thenReturn(List.of(notice));
        when(assignmentReminderRepository.findDueCandidates(NOW, PageRequest.of(0, 100)))
                .thenReturn(List.of(assignment));

        assertThat(reminderService.findCandidates()).containsExactly(notice, assignment);
        verifyNoInteractions(userRepository, noticeRepository, assignmentRepository, notificationService);
    }

    @Test
    @DisplayName("공지 후보만 선점하여 읽지 않은 수신자 알림과 발송 완료 상태를 만든다")
    void processNotice() {
        Study study = mock(Study.class);
        StudyMember recipient = recipient(5L, 7L);
        NoticeReminder reminder = mock(NoticeReminder.class);
        when(studyMemberRepository.findIdentitiesByStudyId(3L))
                .thenReturn(List.of(new StudyMemberIdentityProjection(5L, 7L)));
        Notice notice = mock(Notice.class);
        when(notice.getStudy()).thenReturn(study);
        when(noticeRepository.findByIdAndStudyIdForUpdate(10L, 3L)).thenReturn(Optional.of(notice));
        when(noticeReminderRepository.findClaimableByIdForUpdate(1L, NOW)).thenReturn(List.of(reminder));
        when(noticeRecipientRepository.findUnreadMembersByNoticeId(10L)).thenReturn(List.of(recipient));

        reminderService.process(new ReminderCandidateProjection(ResourceType.NOTICE, 1L, 10L, 3L));

        verify(reminder).markAsProcessing();
        verify(notificationService).createReminderEventNotifications(study, List.of(recipient), 10L, ResourceType.NOTICE);
        verify(reminder).markAsSent();
        verifyNoInteractions(assignmentReminderRepository, assignmentSubmissionRepository);
    }

    @Test
    @DisplayName("과제 후보는 현재 미제출자에게만 알림을 만들고 발송 완료 처리한다")
    void processAssignment() {
        Study study = mock(Study.class);
        StudyMember recipient = recipient(5L, 7L);
        AssignmentReminder reminder = mock(AssignmentReminder.class);
        when(studyMemberRepository.findIdentitiesByStudyId(3L))
                .thenReturn(List.of(new StudyMemberIdentityProjection(5L, 7L)));
        Assignment assignment = mock(Assignment.class);
        when(assignment.getStudy()).thenReturn(study);
        when(assignmentRepository.findByIdAndStudyIdForUpdate(20L, 3L)).thenReturn(Optional.of(assignment));
        when(assignmentReminderRepository.findClaimableByIdForUpdate(2L, NOW)).thenReturn(List.of(reminder));
        when(assignmentSubmissionRepository.findUnsubmittedMembersByAssignmentId(20L)).thenReturn(List.of(recipient));

        reminderService.process(new ReminderCandidateProjection(ResourceType.ASSIGNMENT, 2L, 20L, 3L));

        verify(reminder).markAsProcessing();
        verify(notificationService).createReminderEventNotifications(study, List.of(recipient), 20L,
                ResourceType.ASSIGNMENT);
        verify(reminder).markAsSent();
        verifyNoInteractions(noticeReminderRepository, noticeRecipientRepository);
    }

    @Test
    @DisplayName("탐색 뒤 새로운 멤버십이 수신자가 되면 상태를 바꾸지 않고 다음 탐색으로 넘긴다")
    void skipRecipientOutsideSnapshot() {
        StudyMember recipient = recipient(6L, 7L);
        NoticeReminder reminder = mock(NoticeReminder.class);
        when(studyMemberRepository.findIdentitiesByStudyId(3L))
                .thenReturn(List.of(new StudyMemberIdentityProjection(5L, 7L)));
        when(noticeRepository.findByIdAndStudyIdForUpdate(10L, 3L)).thenReturn(Optional.of(mock(Notice.class)));
        when(noticeReminderRepository.findClaimableByIdForUpdate(1L, NOW)).thenReturn(List.of(reminder));
        when(noticeRecipientRepository.findUnreadMembersByNoticeId(10L)).thenReturn(List.of(recipient));

        reminderService.process(new ReminderCandidateProjection(ResourceType.NOTICE, 1L, 10L, 3L));

        verifyNoInteractions(reminder, notificationService);
    }

    @Test
    @DisplayName("이미 삭제됐거나 선점할 수 없는 후보에는 알림을 만들지 않는다")
    void skipUnavailableCandidate() {
        when(noticeRepository.findByIdAndStudyIdForUpdate(10L, 3L)).thenReturn(Optional.of(mock(Notice.class)));
        when(noticeReminderRepository.findClaimableByIdForUpdate(1L, NOW)).thenReturn(List.of());

        reminderService.process(new ReminderCandidateProjection(ResourceType.NOTICE, 1L, 10L, 3L));

        verifyNoInteractions(notificationService, noticeRecipientRepository);
    }

    @Test
    @DisplayName("원문이 삭제된 후보에는 선점이나 알림 저장을 시도하지 않는다")
    void skipDeletedResource() {

        reminderService.process(new ReminderCandidateProjection(ResourceType.NOTICE, 1L, 10L, 3L));

        verifyNoInteractions(noticeReminderRepository, assignmentReminderRepository, notificationService);
    }

    private StudyMember recipient(Long memberId, Long userId) {
        User user = User.create("수신자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        StudyMember member = StudyMember.create(mock(Study.class), user, "수신자", null, StudyMemberRole.MEMBER);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
