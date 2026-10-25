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
        name = "assignment_attachments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_assignment_attachment_file", columnNames = "file_id"),
                @UniqueConstraint(name = "uk_assignment_attachment_order", columnNames = {"assignment_id", "sort_order"})
        }
)
public class AssignmentAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static AssignmentAttachment create(Assignment assignment, FileMetadata file, int sortOrder) {
        return new AssignmentAttachment(assignment, file, sortOrder);
    }

    private AssignmentAttachment(Assignment assignment, FileMetadata file, int sortOrder) {
        validateAttachment(assignment, file, sortOrder);
        this.assignment = assignment;
        this.file = file;
        this.sortOrder = sortOrder;
    }

    private static void validateAttachment(Assignment assignment, FileMetadata file, int sortOrder) {
        if (assignment == null || file == null || sortOrder < 0) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_ATTACHMENT);
        }
    }
}
