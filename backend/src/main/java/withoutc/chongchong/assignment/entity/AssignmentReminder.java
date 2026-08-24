package withoutc.chongchong.assignment.entity;

import jakarta.persistence.Column;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.global.persistence.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "assignment_reminders")
public class AssignmentReminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentReminderStatus status;

    public static AssignmentReminder create(Assignment assignment, LocalDateTime remindAt, LocalDateTime now) {
        validateRemindAt(remindAt, now);

        return new AssignmentReminder(assignment, remindAt, AssignmentReminderStatus.PENDING);
    }

    public void markAsSent() {
        this.status = AssignmentReminderStatus.SENT;
    }

    public boolean isPending() {
        return status == AssignmentReminderStatus.PENDING;
    }

    private static void validateRemindAt(LocalDateTime remindAt, LocalDateTime now) {
        if (remindAt == null || !remindAt.isAfter(now)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_REMIND_AT);
        }
    }

    private AssignmentReminder(Assignment assignment, LocalDateTime remindAt, AssignmentReminderStatus status) {
        this.assignment = assignment;
        this.remindAt = remindAt;
        this.status = status;
    }
}
