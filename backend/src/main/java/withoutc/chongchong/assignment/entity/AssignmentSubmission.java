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

import jakarta.validation.constraints.Size;
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


    public static AssignmentSubmission create(StudyMember member, Assignment assignment) {
        return new AssignmentSubmission(member, assignment, null, null, false);
    }

    public void submit(String content, String link) {
        validateContent(content);
        validateLink(link);

        this.content = content;
        this.link = link;
        this.submitted = true;
    }

    private AssignmentSubmission(StudyMember member, Assignment assignment, String content, String link,
                                 boolean submitted) {
        this.member = member;
        this.assignment = assignment;
        this.content = content;
        this.link = link;
        this.submitted = submitted;
    }

    private void validateContent(String content) {
        if (content.length() > 10000) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_CONTENT);
        }
    }

    private void validateLink(String link) {
        if (link.length() > 10000) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_LINK);
        }
    }
}
