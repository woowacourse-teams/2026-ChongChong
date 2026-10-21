package withoutc.chongchong.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.global.persistence.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "submission_attachments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_submission_attachment_file", columnNames = "file_id"),
                @UniqueConstraint(name = "uk_submission_attachment_order", columnNames = {"submission_id", "sort_order"})
        }
)
public class SubmissionAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private AssignmentSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static SubmissionAttachment create(AssignmentSubmission submission, FileMetadata file, int sortOrder) {
        return new SubmissionAttachment(submission, file, sortOrder);
    }

    private SubmissionAttachment(AssignmentSubmission submission, FileMetadata file, int sortOrder) {
        validateAttachment(submission, file, sortOrder);
        this.submission = submission;
        this.file = file;
        this.sortOrder = sortOrder;
    }

    private static void validateAttachment(AssignmentSubmission submission, FileMetadata file, int sortOrder) {
        if (submission == null || file == null || sortOrder < 0) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_ATTACHMENT);
        }
    }
}
