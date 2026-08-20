package withoutc.chongchong.notice.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_reminders")
public class NoticeReminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeReminderStatus status;

    public static NoticeReminder create(Notice notice, LocalDateTime remindAt, LocalDateTime now) {
        validateRemindAt(remindAt, now);

        return new NoticeReminder(notice, remindAt, NoticeReminderStatus.PENDING);
    }

    public void markAsSent() {
        this.status = NoticeReminderStatus.SENT;
    }

    public boolean isPending() {
        return status == NoticeReminderStatus.PENDING;
    }

    private static void validateRemindAt(LocalDateTime remindAt, LocalDateTime now) {
        if (remindAt == null || !remindAt.isAfter(now)) {
            throw new NoticeException(NoticeErrorCode.INVALID_REMIND_AT);
        }
    }

    private NoticeReminder(Notice notice, LocalDateTime remindAt, NoticeReminderStatus status) {
        this.notice = notice;
        this.remindAt = remindAt;
        this.status = status;
    }
}
