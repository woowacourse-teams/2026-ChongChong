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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.study.entity.StudyMember;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notice_recipients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notice_recipients_notice_member",
                columnNames = {"notice_id", "member_id"}
        )
)
public class NoticeRecipient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private StudyMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "read_at", nullable = true)
    private LocalDateTime readAt;

    public static NoticeRecipient create(StudyMember member, Notice notice) {
        return new NoticeRecipient(member, notice, null);
    }

    private NoticeRecipient(StudyMember member, Notice notice, LocalDateTime readAt) {
        this.member = member;
        this.notice = notice;
        this.readAt = readAt;
    }

    public void markAsRead(LocalDateTime now) {
        if (this.readAt == null) {
            this.readAt = now.truncatedTo(ChronoUnit.MICROS);
        }
    }

    public boolean isRead() {
        return readAt != null;
    }
}
