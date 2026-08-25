package withoutc.chongchong.assignment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Entity
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private StudyMember writer;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "submission_method")
    private String submissionMethod;

    @Column(name = "close_at", nullable = false)
    private LocalDateTime closeAt;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AssignmentReminder> reminders = new ArrayList<>();

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AssignmentRecipient> recipients = new ArrayList<>();

    public static Assignment create(StudyMember writer, String title, String content,
                                    String submissionMethod,
                                    LocalDateTime closeAt, Clock clock) {
        return new Assignment(writer, title, content, submissionMethod, closeAt, LocalDateTime.now(clock));
    }

    private Assignment(StudyMember writer, String title, String content, String submissionMethod,
                       LocalDateTime closeAt, LocalDateTime now) {
        validateTitle(title);
        validateContent(content);
        validateSubmissionMethod(submissionMethod);
        validateCloseAt(closeAt, now);

        this.study = writer.getStudy();
        this.writer = writer;
        this.title = title;
        this.content = content;
        this.submissionMethod = submissionMethod;
        this.closeAt = closeAt;
    }

    public void update(String title, String content, String submissionMethod, LocalDateTime closeAt,
                       List<LocalDateTime> remindAts, Clock clock) {
        if (title != null) {
            validateTitle(title);
            this.title = title;
        }
        if (content != null) {
            validateContent(content);
            this.content = content;
        }
        if (submissionMethod != null) {
            validateSubmissionMethod(submissionMethod);
            this.submissionMethod = submissionMethod;
        }
        if (closeAt != null) {
            validateCloseAt(closeAt, LocalDateTime.now(clock));
        }

        if (remindAts != null) {
            replacePendingReminders(remindAts, LocalDateTime.now(clock));
        }
    }

    public void replacePendingReminders(List<LocalDateTime> remindAts, LocalDateTime now) {
        if (remindAts.stream().anyMatch(Objects::isNull)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_REMIND_AT);
        }

        List<AssignmentReminder> newReminders = remindAts.stream().distinct()
                .map(remindAt -> AssignmentReminder.create(this, remindAt, now)).toList();

        reminders.removeIf(AssignmentReminder::isPending);
        reminders.addAll(newReminders);
    }

    public int getRecipientCount() {
        return this.recipients.size();
    }

    public int getSubmitCount() {
        return Math.toIntExact(this.recipients.stream().filter(AssignmentRecipient::isSubmit).count());
    }

    public LocalDateTime getNextRemindAt() {
        return reminders.stream().filter(AssignmentReminder::isPending).map(AssignmentReminder::getRemindAt)
                .min(LocalDateTime::compareTo).orElse(null);
    }

    public void addRecipients(List<StudyMember> members) {
        Set<Long> recipientMemberIds = recipients.stream()
                .map(recipient -> recipient.getMember().getId())
                .collect(Collectors.toCollection(HashSet::new));

        members.stream()
                .filter(member -> member.getId() != null)
                .filter(member -> recipientMemberIds.add(member.getId()))
                .map(member -> AssignmentRecipient.create(member, this))
                .forEach(this.recipients::add);
    }

    public void addReminders(List<LocalDateTime> remindAts, LocalDateTime now) {
        if (remindAts == null) {
            return;
        }

        remindAts.stream()
                .distinct()
                .map(remindAt -> AssignmentReminder.create(this, remindAt, now))
                .forEach(this.reminders::add);
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 15) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > 10000) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_CONTENT);
        }
    }

    private static void validateSubmissionMethod(String submissionMethod) {
        if (submissionMethod == null || submissionMethod.isBlank() || submissionMethod.length() > 10000) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_CONTENT);
        }
    }

    private static void validateCloseAt(LocalDateTime closeAt, LocalDateTime now) {
        if (closeAt.isBefore(now)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_REMIND_AT);
        }

    }
}
