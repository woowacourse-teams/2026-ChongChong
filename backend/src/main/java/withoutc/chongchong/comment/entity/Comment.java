package withoutc.chongchong.comment.entity;

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
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.comment.exception.CommentException;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.user.entity.User;

@Entity
@Getter
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final long MIN_RESOURCE_ID = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_member_id")
    private StudyMember authorMember;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    public static Comment create(ResourceType resourceType, Long resourceId, StudyMember author, String content) {
        return new Comment(resourceType, resourceId, author, content);
    }

    private Comment(ResourceType resourceType, Long resourceId, StudyMember author, String content) {
        if (resourceType == null || resourceId == null || resourceId < MIN_RESOURCE_ID
                || author == null || author.getUser() == null) {
            throw new CommentException(CommentErrorCode.INVALID_COMMENT);
        }
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.authorUser = author.getUser();
        this.authorMember = author;
        this.content = normalize(content);
    }

    public void update(String content, LocalDateTime now) {
        String normalized = normalize(content);
        if (this.content.equals(normalized)) {
            return;
        }
        this.content = normalized;
        this.editedAt = now.truncatedTo(ChronoUnit.MICROS);
    }

    public Long authorUserId() {
        if (authorUser == null) {
            return null;
        }
        return authorUser.getId();
    }

    private static String normalize(String content) {
        if (content == null) {
            throw new CommentException(CommentErrorCode.INVALID_CONTENT);
        }
        String normalized = content.trim();
        if (normalized.isBlank() || normalized.length() > MAX_CONTENT_LENGTH) {
            throw new CommentException(CommentErrorCode.INVALID_CONTENT);
        }
        return normalized;
    }
}
