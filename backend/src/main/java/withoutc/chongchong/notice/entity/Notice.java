package withoutc.chongchong.notice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private StudyMember writer;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static Notice create(Study study, StudyMember member, String title, String content) {
        return new Notice(study, member, title, content);
    }

    private Notice(Study study, StudyMember writer, String title, String content) {
        validateTitle(title);
        validateContent(content);

        this.study = study;
        this.writer = writer;
        this.title = title;
        this.content = content;
    }

    private static void validateTitle(String title) {
        if (title.isBlank()) {
            throw new NoticeException(NoticeErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content.isBlank() || content.length() >= 10000) {
            throw new NoticeException(NoticeErrorCode.INVALID_CONTENT);
        }
    }
}
