package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentRecurrence;
import withoutc.chongchong.assignment.entity.RecurrenceSchedule;
import withoutc.chongchong.assignment.logging.AssignmentRecurrenceLogger;
import withoutc.chongchong.assignment.repository.AssignmentRecurrenceRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentRecurrenceCandidate;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.user.repository.UserRepository;

// 직전 회차가 마감된 반복 규칙마다 다음 회차를 만든다(ADR-0064).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentRecurrenceGenerationService {

    private final AssignmentRecurrenceRepository assignmentRecurrenceRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AssignmentRecurrenceLogger logger;
    private final Clock clock;

    public List<AssignmentRecurrenceCandidate> findDueCandidates(int batchSize) {
        return assignmentRecurrenceRepository.findDueCandidates(LocalDateTime.now(clock),
                PageRequest.of(0, batchSize));
    }

    // 잠금 순서는 과제 생성·스터디 삭제와 같은 유저 → 스터디 → 규칙이다. 규칙을 먼저 잠그면 스터디 삭제와 교착될 수 있다.
    @Transactional
    public void generate(AssignmentRecurrenceCandidate candidate) {
        List<StudyMemberIdentityProjection> members = lockMembers(candidate.studyId());
        if (studyRepository.findByIdForShare(candidate.studyId()).isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<AssignmentRecurrence> due = assignmentRecurrenceRepository
                .findDueByIdForUpdate(candidate.recurrenceId(), now);
        if (due.isEmpty()) {
            return;
        }
        AssignmentRecurrence recurrence = due.getFirst();
        logSkipped(recurrence.getId(), recurrence.skipPastOccurrences(now));
        generateOccurrence(recurrence, currentMembers(candidate.studyId(), members), now);
        recurrence.advance();
    }

    // 같은 회차가 이미 있어 유니크 제약에 걸린 규칙은 그 회차를 건너뛰고 다음 차례로 넘긴다.
    @Transactional
    public void skipExistingOccurrence(AssignmentRecurrenceCandidate candidate) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<AssignmentRecurrence> due = assignmentRecurrenceRepository
                .findDueByIdForUpdate(candidate.recurrenceId(), now);
        if (due.isEmpty()) {
            return;
        }
        AssignmentRecurrence recurrence = due.getFirst();
        recurrence.skipPastOccurrences(now);
        RecurrenceSchedule schedule = recurrence.getSchedule();
        if (assignmentRepository.existsByRecurrenceIdAndOccurrenceDate(recurrence.getId(),
                schedule.getNextOccurrenceDate())) {
            recurrence.advance();
        }
    }

    // 생성에 실패한 규칙은 잠시 뒤로 미뤄 정상 규칙에 배치 자리를 내준다. 고쳐지면 다음 시도에서 생성된다.
    @Transactional
    public void postpone(AssignmentRecurrenceCandidate candidate, Duration delay) {
        LocalDateTime now = LocalDateTime.now(clock);
        assignmentRecurrenceRepository.findDueByIdForUpdate(candidate.recurrenceId(), now)
                .forEach(recurrence -> recurrence.postponeGeneration(now.plus(delay)));
    }

    private List<StudyMemberIdentityProjection> lockMembers(Long studyId) {
        List<StudyMemberIdentityProjection> members = studyMemberRepository.findIdentitiesByStudyId(studyId);
        userRepository.findAllByIdInForShare(members.stream().map(StudyMemberIdentityProjection::userId)
                .distinct().sorted().toList());
        return members;
    }

    // 대상이 0명이면 회차를 만들지 않고 넘어간다.
    private void generateOccurrence(AssignmentRecurrence recurrence, List<StudyMember> studyMembers,
                                    LocalDateTime now) {
        List<StudyMember> submitters = studyMembers.stream().filter(recurrence::includes).toList();
        if (submitters.isEmpty()) {
            logger.noSubmitters(recurrence.getId());
            return;
        }
        Assignment occurrence = assignmentRepository.saveAndFlush(recurrence.createOccurrence(submitters, now));
        notificationService.createAssignmentCreatedEventNotifications(occurrence,
                withLeaders(submitters, studyMembers));
        logger.occurrenceCreated(recurrence.getId(), occurrence.getId(), occurrence.getOccurrenceDate());
    }

    private void logSkipped(Long recurrenceId, int skipped) {
        if (skipped > 0) {
            logger.pastOccurrencesSkipped(recurrenceId, skipped);
        }
    }

    // 잠금 전에 읽은 멤버 중 잠금 후에도 남아 있는 멤버만 쓴다.
    private List<StudyMember> currentMembers(Long studyId, List<StudyMemberIdentityProjection> members) {
        return studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(member -> members.stream().anyMatch(identity -> identity.matches(member)))
                .toList();
    }

    // 자동 생성 회차는 리더가 제출 대상이 아니어도 생성 알림을 받는다.
    private List<StudyMember> withLeaders(List<StudyMember> submitters, List<StudyMember> studyMembers) {
        return Stream.concat(submitters.stream(), studyMembers.stream().filter(StudyMember::isLeader))
                .distinct()
                .toList();
    }
}
