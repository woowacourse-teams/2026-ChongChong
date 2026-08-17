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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.study.entity.Study;

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

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "submission_method")
    private String submissionMethod;

    @Column(name = "close_at", nullable = false)
    private LocalDateTime closeAt;

    public static Assignment create(
            Study study,
            String title,
            String content,
            String submissionMethod,
            LocalDateTime closeAt
    ) {
        return new Assignment(study, title, content, submissionMethod, closeAt);
    }

    private Assignment(
            Study study,
            String title,
            String content,
            String submissionMethod,
            LocalDateTime closeAt
    ) {
        this.study = study;
        this.title = title;
        this.content = content;
        this.submissionMethod = submissionMethod;
        this.closeAt = closeAt;
    }
}
