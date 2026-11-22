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
        recurrence.addRemindOffsets(firstOccurrence);
        recurrence.addSubmitterIds(firstOccurrence);
        return recurrence;
    }

    public boolean selectsSubmitters() {
        return submitterMode == SubmitterMode.SELECTED;
    }

    public Assignment createOccurrence(List<StudyMember> submitters, LocalDateTime now) {
        LocalDateTime closeAt = schedule.nextCloseAt();
        Assignment occurrence = Assignment.create(study, title, content, submissionMethod, submissionTarget,
                submissionVisibility, closeAt, now, submitters);
        occurrence.linkRecurrence(id, schedule.getNextOccurrenceDate());
        occurrence.addReminders(futureRemindAts(closeAt, now), now);
        return occurrence;
    }

    public int skipPastOccurrences(LocalDateTime now) {
        return schedule.skipPastOccurrences(now);
    }

    public void advance() {
        schedule.advance();
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

    private void addRemindOffsets(Assignment firstOccurrence) {
        LocalDateTime closeAt = firstOccurrence.getCloseAt();
        for (AssignmentReminder reminder : firstOccurrence.getReminders()) {
            remindOffsets.add(toRemindOffset(reminder.getRemindAt(), closeAt));
        }
    }

    private int toRemindOffset(LocalDateTime remindAt, LocalDateTime closeAt) {
        long minutesBeforeClose = Duration.between(remindAt, closeAt).toMinutes();
        if (!schedule.allowsRemindOffset(minutesBeforeClose)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_REMIND_AT);
        }
        return Math.toIntExact(minutesBeforeClose);
    }

    private void addSubmitterIds(Assignment firstOccurrence) {
        if (selectsSubmitters()) {
            submitterIds.addAll(firstOccurrence.getSubmitterIds());
        }
    }

    private List<LocalDateTime> futureRemindAts(LocalDateTime closeAt, LocalDateTime now) {
        return remindOffsets.stream()
                .map(closeAt::minusMinutes)
                .filter(remindAt -> remindAt.isAfter(now))
                .toList();
    }
}
