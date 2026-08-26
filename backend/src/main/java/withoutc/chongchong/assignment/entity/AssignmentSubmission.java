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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.study.entity.StudyMember;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "assignment_submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignment_submissions_assignment_member",
                columnNames = {"assignment_id", "member_id"}
        )
)
public class AssignmentSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private StudyMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String link;

    @Column(nullable = false)
    private boolean submitted;

    private LocalDateTime submittedAt;

    public static AssignmentSubmission create(StudyMember member, Assignment assignment) {
        return new AssignmentSubmission(member, assignment, null, null, false, null);
    }

    public void submit(String content, String link, LocalDateTime now) {
        validateContent(content);
        validateLink(link);

        this.content = content;
        this.link = link;
        this.submitted = true;
        if (submittedAt == null) {
            this.submittedAt = now.truncatedTo(ChronoUnit.MICROS);
        }
    }

    public void update(String content, String link) {
        if (content != null) {
            validateContent(content);
            this.content = content;
        }

        if (link != null) {
            validateLink(link);
            this.link = link;
        }
    }

    private AssignmentSubmission(StudyMember member, Assignment assignment, String content, String link,
                                 boolean submitted, LocalDateTime submittedAt) {
        this.member = member;
        this.assignment = assignment;
        this.content = content;
        this.link = link;
        this.submitted = submitted;
        this.submittedAt = submittedAt;
    }

    private void validateContent(String content) {
        if (content != null && content.length() > 10000) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_CONTENT);
        }
    }

    private void validateLink(String link) {
        if (link != null && link.length() > 10000) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_LINK);
        }
    }
}
