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
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.study.entity.StudyMember;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "assignment_recipients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignment_recipients_assignment_member",
                columnNames = {"assignment_id", "member_id"}
        )
)
public class AssignmentRecipient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private StudyMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(nullable = true)
    private String content;

    @Column(nullable = true)
    private String link;

    @Column(nullable = false)
    private boolean isSubmit;


    public static AssignmentRecipient create(StudyMember member, Assignment assignment) {
        return new AssignmentRecipient(member, assignment, null, null, false);
    }

    private AssignmentRecipient(StudyMember member, Assignment assignment, String content, String link,
                                boolean isSubmit) {
        this.member = member;
        this.assignment = assignment;
        this.content = content;
        this.link = link;
        this.isSubmit = isSubmit;
    }
}
