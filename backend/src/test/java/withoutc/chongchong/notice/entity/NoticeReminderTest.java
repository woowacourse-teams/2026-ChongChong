package withoutc.chongchong.notice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;

class NoticeReminderTest {

    private final Notice notice = mock(Notice.class);
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 21, 10, 0);

    @Test
    @DisplayName("미래 시각으로 리마인더를 생성하면 발송 대기 상태가 된다")
    void createTest() {
        LocalDateTime remindAt = now.plusHours(1);

        NoticeReminder reminder = NoticeReminder.create(notice, remindAt, now);

        assertThat(reminder.getNotice()).isSameAs(notice);
        assertThat(reminder.getRemindAt()).isEqualTo(remindAt);
        assertThat(reminder.getStatus()).isEqualTo(NoticeReminderStatus.PENDING);
        assertThat(reminder.isPending()).isTrue();
    }

    @Test
    @DisplayName("null인 시각으로 리마인더를 생성할 수 없다")
    void createWithNullRemindAtTest() {
        assertInvalidRemindAt(null);
    }

    @Test
    @DisplayName("현재 시각으로 리마인더를 생성할 수 없다")
    void createWithCurrentRemindAtTest() {
        assertInvalidRemindAt(now);
    }

    @Test
    @DisplayName("과거 시각으로 리마인더를 생성할 수 없다")
    void createWithPastRemindAtTest() {
        assertInvalidRemindAt(now.minusNanos(1));
    }

    @Test
    @DisplayName("리마인더를 발송 완료 처리하면 더 이상 발송 대기 상태가 아니다")
    void markAsSentTest() {
        NoticeReminder reminder = NoticeReminder.create(notice, now.plusHours(1), now);

        reminder.markAsSent();

        assertThat(reminder.getStatus()).isEqualTo(NoticeReminderStatus.SENT);
        assertThat(reminder.isPending()).isFalse();
    }

    private void assertInvalidRemindAt(LocalDateTime remindAt) {
        assertThatThrownBy(() -> NoticeReminder.create(notice, remindAt, now))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_REMIND_AT);
    }
}
