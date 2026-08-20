package withoutc.chongchong.notice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
@Table(name = "notices")
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

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<NoticeReminder> reminders = new ArrayList<>();

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<NoticeRecipient> recipients = new ArrayList<>();

    public static Notice create(Study study, StudyMember member, String title, String content) {
        return new Notice(study, member, title, content);
    }

    public void addRecipients(List<StudyMember> members) {
        members.stream()
                .map(member -> NoticeRecipient.create(member, this))
                .forEach(this.recipients::add);
    }

    public void addReminders(List<LocalDateTime> remindAts, LocalDateTime now) {
        if (remindAts == null) {
            return;
        }

        remindAts.stream()
                .distinct()
                .map(remindAt -> NoticeReminder.create(this, remindAt, now))
                .forEach(this.reminders::add);
    }

    public void update(String title, String content, List<LocalDateTime> remindAts, LocalDateTime now) {
        if (title != null) {
            validateTitle(title);
            this.title = title;
        }
        if (content != null) {
            validateContent(content);
            this.content = content;
        }
        if (remindAts != null) {
            replacePendingReminders(remindAts, now);
        }
    }

    public void replacePendingReminders(List<LocalDateTime> remindAts, LocalDateTime now) {
        if (remindAts.stream().anyMatch(Objects::isNull)) {
            throw new NoticeException(NoticeErrorCode.INVALID_REMIND_AT);
        }

        List<NoticeReminder> newReminders = remindAts.stream()
                .distinct()
                .map(remindAt -> NoticeReminder.create(this, remindAt, now))
                .toList();

        reminders.removeIf(NoticeReminder::isPending);
        reminders.addAll(newReminders);
    }

    public int getRecipientsCount() {
        return this.recipients.size();
    }

    public int getReadCount() {
        return Math.toIntExact(this.recipients.stream()
                .filter(NoticeRecipient::isRead)
                .count()
        );
    }

    public LocalDateTime getLastRemindAt() {
        if (reminders.isEmpty()) {
            return null;
        }

        return reminders.stream()
                .map(NoticeReminder::getRemindAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
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
        if (title.isBlank() || title.length() > 15) {
            throw new NoticeException(NoticeErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content.isBlank() || content.length() > 10000) {
            throw new NoticeException(NoticeErrorCode.INVALID_CONTENT);
        }
    }
}
