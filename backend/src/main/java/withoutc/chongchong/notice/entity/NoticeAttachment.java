package withoutc.chongchong.notice.entity;

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
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.global.persistence.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notice_attachments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notice_attachment_file", columnNames = "file_id"),
                @UniqueConstraint(name = "uk_notice_attachment_order", columnNames = {"notice_id", "sort_order"})
        }
)
public class NoticeAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static NoticeAttachment create(Notice notice, FileMetadata file, int sortOrder) {
        return new NoticeAttachment(notice, file, sortOrder);
    }

    private NoticeAttachment(Notice notice, FileMetadata file, int sortOrder) {
        if (sortOrder < 0) {
            throw new IllegalArgumentException("첨부 표시 순서는 0 이상이어야 합니다.");
        }
        this.notice = Objects.requireNonNull(notice, "공지는 필수 값입니다.");
        this.file = Objects.requireNonNull(file, "첨부 파일은 필수 값입니다.");
        this.sortOrder = sortOrder;
    }
}
