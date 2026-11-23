package withoutc.chongchong.assignment.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Entity
@Table(name = "assignment_recurrences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentRecurrence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "submission_method", columnDefinition = "TEXT")
    private String submissionMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_visibility", nullable = false)
    private SubmissionVisibility submissionVisibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "submitter_mode", nullable = false)
    private SubmitterMode submitterMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_target")
    private SubmissionTarget submissionTarget;

    @Embedded
    private RecurrenceSchedule schedule;

    @ElementCollection
    @CollectionTable(name = "assignment_recurrence_reminders", joinColumns = @JoinColumn(name = "recurrence_id"))
    @Column(name = "minutes_before_close", nullable = false)
    private final Set<Integer> remindOffsets = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "assignment_recurrence_submitters", joinColumns = @JoinColumn(name = "recurrence_id"))
    @Column(name = "member_id", nullable = false)
    private final Set<Long> submitterIds = new HashSet<>();

    // 리더가 만든 첫 회차를 템플릿으로 삼는다. 첫 회차의 리마인드 시각은 마감 기준 상대 시간으로 바꿔 저장한다.
    public static AssignmentRecurrence startWith(Assignment firstOccurrence, int intervalWeeks,
                                                 SubmitterMode submitterMode) {
        AssignmentRecurrence recurrence = new AssignmentRecurrence(firstOccurrence, submitterMode,
                RecurrenceSchedule.startAfter(firstOccurrence.getCloseAt(), intervalWeeks));
        recurrence.addRemindOffsets(firstOccurrence.getPendingRemindAts(LocalDateTime.MIN),
                firstOccurrence.getCloseAt());
        recurrence.addSubmitterIds(firstOccurrence.getSubmitterIds());
        return recurrence;
    }

    public boolean selectsSubmitters() {
        return submitterMode == SubmitterMode.SELECTED;
    }

    public boolean includes(StudyMember member) {
        if (selectsSubmitters()) {
            return submitterIds.contains(member.getId());
        }
        return submissionTarget.requiresLeaderSubmission() || !member.isLeader();
    }

    public Assignment createOccurrence(List<StudyMember> submitters, LocalDateTime now) {
        LocalDateTime closeAt = schedule.nextCloseAt();
        Assignment occurrence = Assignment.create(study, title, content, submissionMethod, submissionTarget,
                submissionVisibility, closeAt, now, submitters);
        occurrence.linkRecurrence(id);
        occurrence.addReminders(remindAtsFor(closeAt, now), now);
        return occurrence;
    }

    public int skipPastOccurrences(LocalDateTime now) {
        return schedule.skipPastOccurrences(now);
    }

    public void advance() {
        schedule.advance();
    }

    public void postponeGeneration(LocalDateTime until) {
        schedule.postponeGeneration(until);
    }

    public List<LocalDateTime> nextRemindAts() {
        return remindAtsFor(schedule.nextCloseAt(), LocalDateTime.MIN);
    }

    public void changeTemplate(String title, String content, String submissionMethod) {
        if (title != null) {
            Assignment.validateTitle(title);
            this.title = title;
        }
        if (content != null) {
            Assignment.validateContent(content);
            this.content = content;
        }
        if (submissionMethod != null) {
            Assignment.validateSubmissionMethod(submissionMethod);
            this.submissionMethod = Assignment.normalizeSubmissionMethod(submissionMethod);
        }
    }

    public void changeSubmissionVisibility(SubmissionVisibility submissionVisibility) {
        if (submissionVisibility != null) {
            this.submissionVisibility = submissionVisibility;
        }
    }

    public void useRange(SubmissionTarget submissionTarget) {
        this.submitterMode = SubmitterMode.RANGE;
        this.submissionTarget = submissionTarget;
        submitterIds.clear();
    }

    public void selectSubmitters(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_SUBMITTER_IDS);
        }
        this.submitterMode = SubmitterMode.SELECTED;
        this.submissionTarget = null;
        submitterIds.clear();
        submitterIds.addAll(memberIds);
    }

    // 간격을 생략하면 기존 간격을 유지한다. 줄어든 간격에도 기존 리마인드가 맞는지 다시 확인한다.
    public void reschedule(LocalDateTime nextCloseAt, Integer intervalWeeks) {
        if (nextCloseAt == null && intervalWeeks == null) {
            return;
        }
        if (nextCloseAt == null) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_CLOSE_AT);
        }
        schedule.reschedule(nextCloseAt, Objects.requireNonNullElse(intervalWeeks, schedule.getIntervalWeeks()));
        remindOffsets.forEach(this::validateRemindOffset);
    }

    // 다음 회차 기준 절대 시각을 받아 마감 기준 상대 시간으로 교체한다.
    public void replaceRemindAts(List<LocalDateTime> remindAts) {
        if (remindAts == null) {
            return;
        }
        remindOffsets.clear();
        addRemindOffsets(remindAts, schedule.nextCloseAt());
    }

    private AssignmentRecurrence(Assignment firstOccurrence, SubmitterMode submitterMode,
                                 RecurrenceSchedule schedule) {
        this.study = firstOccurrence.getStudy();
        this.title = firstOccurrence.getTitle();
        this.content = firstOccurrence.getContent();
        this.submissionMethod = firstOccurrence.getSubmissionMethod();
        this.submissionVisibility = firstOccurrence.getSubmissionVisibility();
        this.submitterMode = submitterMode;
        this.schedule = schedule;
        if (submitterMode == SubmitterMode.RANGE) {
            this.submissionTarget = firstOccurrence.getSubmissionTarget();
        }
    }

    private void addRemindOffsets(List<LocalDateTime> remindAts, LocalDateTime closeAt) {
        for (LocalDateTime remindAt : remindAts) {
            long minutesBeforeClose = Duration.between(remindAt, closeAt).toMinutes();
            validateRemindOffset(minutesBeforeClose);
            remindOffsets.add(Math.toIntExact(minutesBeforeClose));
        }
    }

    private void validateRemindOffset(long minutesBeforeClose) {
        if (!schedule.allowsRemindOffset(minutesBeforeClose)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_REMIND_AT);
        }
    }

    private void addSubmitterIds(List<Long> memberIds) {
        if (selectsSubmitters()) {
            submitterIds.addAll(memberIds);
        }
    }

    private List<LocalDateTime> remindAtsFor(LocalDateTime closeAt, LocalDateTime now) {
        return remindOffsets.stream()
                .map(closeAt::minusMinutes)
                .filter(remindAt -> remindAt.isAfter(now))
                .sorted()
                .toList();
    }
}
