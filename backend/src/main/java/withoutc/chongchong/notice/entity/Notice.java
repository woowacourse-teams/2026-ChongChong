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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<NoticeReminder> reminders = new ArrayList<>();

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<NoticeRecipient> recipients = new ArrayList<>();

    public static Notice create(Study study, String title, String content) {
        return new Notice(study, title, content);
    }

    public void addRecipients(List<StudyMember> members) {
        Set<Long> recipientMemberIds = recipients.stream().map(recipient -> recipient.getMember().getId())
                .collect(Collectors.toCollection(HashSet::new));

        members.stream().filter(member -> member.getId() != null)
                .filter(member -> recipientMemberIds.add(member.getId()))
                .map(member -> NoticeRecipient.create(member, this)).forEach(this.recipients::add);
    }

    public void addReminders(List<LocalDateTime> remindAts, LocalDateTime now) {
        if (remindAts == null) {
            return;
        }

        remindAts.stream().distinct().map(remindAt -> NoticeReminder.create(this, remindAt, now))
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

        List<NoticeReminder> newReminders = remindAts.stream().distinct()
                .map(remindAt -> NoticeReminder.create(this, remindAt, now)).toList();

        reminders.removeIf(NoticeReminder::isPending);
        reminders.addAll(newReminders);
    }

    public int getRecipientCount() {
        return this.recipients.size();
    }

    public int getReadRecipientCount() {
        return Math.toIntExact(this.recipients.stream().filter(NoticeRecipient::isRead).count());
    }

    public LocalDateTime getNextRemindAt() {
        return reminders.stream().filter(NoticeReminder::isPending).map(NoticeReminder::getRemindAt)
                .min(LocalDateTime::compareTo).orElse(null);
    }

    private Notice(Study study, String title, String content) {
        validateTitle(title);
        validateContent(content);

        this.study = study;
        this.title = title;
        this.content = content;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 20) {
            throw new NoticeException(NoticeErrorCode.INVALID_TITLE);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > 10000) {
            throw new NoticeException(NoticeErrorCode.INVALID_CONTENT);
        }
    }
}
