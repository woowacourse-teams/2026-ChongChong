package withoutc.chongchong.notice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.study.entity.StudyMember;

class NoticeRecipientTest {

    @Test
    @DisplayName("공지 수신자는 공지와 스터디원을 참조하며 읽지 않은 상태로 생성된다")
    void createTest() {
        StudyMember member = mock(StudyMember.class);
        Notice notice = mock(Notice.class);

        NoticeRecipient recipient = NoticeRecipient.create(member, notice);

        assertThat(recipient.getMember()).isSameAs(member);
        assertThat(recipient.getNotice()).isSameAs(notice);
        assertThat(recipient.getReadAt()).isNull();
        assertThat(recipient.isRead()).isFalse();
    }

    @Test
    @DisplayName("공지 수신자를 읽음 처리하면 주어진 Clock의 시각을 기록한다")
    void markAsReadTest() {
        NoticeRecipient recipient = NoticeRecipient.create(mock(StudyMember.class), mock(Notice.class));
        LocalDateTime expectedReadAt = LocalDateTime.of(2026, 8, 24, 12, 30);
        Clock clock = Clock.fixed(expectedReadAt.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        recipient.markAsRead(clock);

        assertThat(recipient.getReadAt()).isEqualTo(expectedReadAt);
        assertThat(recipient.isRead()).isTrue();
    }

    @Test
    @DisplayName("공지 수신자를 여러 번 읽음 처리해도 최초 읽은 시각을 유지한다")
    void markAsReadIsIdempotentTest() {
        NoticeRecipient recipient = NoticeRecipient.create(mock(StudyMember.class), mock(Notice.class));
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 8, 24, 12, 30);
        LocalDateTime secondReadAt = firstReadAt.plusMinutes(5);
        Clock firstClock = Clock.fixed(firstReadAt.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        Clock secondClock = Clock.fixed(secondReadAt.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        recipient.markAsRead(firstClock);
        recipient.markAsRead(secondClock);

        assertThat(recipient.getReadAt()).isEqualTo(firstReadAt);
    }
}
