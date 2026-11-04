package withoutc.chongchong.assignment.entity;

import java.time.LocalDateTime;

public enum SubmissionStatus {
    NOT_ASSIGNED,
    NOT_SUBMITTED,
    SUBMITTED,
    LATE_SUBMITTED,
    MISSING;

    public static SubmissionStatus from(LocalDateTime submittedAt, LocalDateTime closeAt, LocalDateTime now) {
        if (submittedAt == null) {
            return now.isAfter(closeAt) ? MISSING : NOT_SUBMITTED;
        }
        return submittedAt.isAfter(closeAt) ? LATE_SUBMITTED : SUBMITTED;
    }
}
