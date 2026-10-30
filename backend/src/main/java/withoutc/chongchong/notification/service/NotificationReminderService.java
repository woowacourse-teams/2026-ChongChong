package withoutc.chongchong.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentReminder;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentReminderRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeReminderRepository;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.projection.ReminderCandidateProjection;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationReminderService {

    private static final int BATCH_SIZE = 100;

    private final NoticeReminderRepository noticeReminderRepository;
    private final AssignmentReminderRepository assignmentReminderRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final NoticeRepository noticeRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public List<ReminderCandidateProjection> findCandidates() {
        LocalDateTime now = LocalDateTime.now(clock);
        PageRequest page = PageRequest.of(0, BATCH_SIZE);
        return Stream.concat(noticeReminderRepository.findDueCandidates(now, page).stream(),
                assignmentReminderRepository.findDueCandidates(now, page).stream()).toList();
    }

    @Transactional
    public void process(ReminderCandidateProjection candidate) {
        List<StudyMemberIdentityProjection> members = studyMemberRepository.findIdentitiesByStudyId(candidate.studyId());
        userRepository.findAllByIdInForShare(members.stream().map(StudyMemberIdentityProjection::userId)
                .distinct().sorted().toList());
        LocalDateTime now = LocalDateTime.now(clock);
        if (candidate.resourceType() == ResourceType.NOTICE) {
            processNotice(candidate, members, now);
        } else if (candidate.resourceType() == ResourceType.ASSIGNMENT) {
            processAssignment(candidate, members, now);
        }
    }

    private void processNotice(ReminderCandidateProjection candidate,
                               List<StudyMemberIdentityProjection> members, LocalDateTime now) {
        Optional<Notice> found = noticeRepository.findByIdAndStudyIdForUpdate(candidate.resourceId(), candidate.studyId());
        if (found.isEmpty()) {
            return;
        }
        Notice notice = found.get();
        List<NoticeReminder> reminders = noticeReminderRepository.findClaimableByIdForUpdate(candidate.reminderId(), now);
        if (reminders.isEmpty()) {
            return;
        }
        List<StudyMember> recipients = noticeRecipientRepository.findUnreadMembersByNoticeId(candidate.resourceId());
        if (recipients.stream().anyMatch(member -> members.stream().noneMatch(identity -> identity.matches(member)))) {
            return;
        }
        NoticeReminder reminder = reminders.getFirst();
        reminder.markAsProcessing();
        notificationService.createReminderEventNotifications(notice.getStudy(), recipients,
                candidate.resourceId(), ResourceType.NOTICE);
        reminder.markAsSent();
    }

    private void processAssignment(ReminderCandidateProjection candidate,
                                   List<StudyMemberIdentityProjection> members, LocalDateTime now) {
        Optional<Assignment> found = assignmentRepository.findByIdAndStudyIdForUpdate(
                candidate.resourceId(), candidate.studyId());
        if (found.isEmpty()) {
            return;
        }
        Assignment assignment = found.get();
        List<AssignmentReminder> reminders = assignmentReminderRepository.findClaimableByIdForUpdate(
                candidate.reminderId(), now);
        if (reminders.isEmpty()) {
            return;
        }
        List<StudyMember> recipients = assignmentSubmissionRepository.findUnsubmittedMembersByAssignmentId(
                candidate.resourceId());
        if (recipients.stream().anyMatch(member -> members.stream().noneMatch(identity -> identity.matches(member)))) {
            return;
        }
        AssignmentReminder reminder = reminders.getFirst();
        reminder.markAsProcessing();
        notificationService.createReminderEventNotifications(assignment.getStudy(), recipients,
                candidate.resourceId(), ResourceType.ASSIGNMENT);
        reminder.markAsSent();
    }
}
