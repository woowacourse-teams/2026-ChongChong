package withoutc.chongchong.notice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    @DisplayName("공지 수신자를 읽음 처리하면 주어진 시각을 기록한다")
    void markAsReadTest() {
        NoticeRecipient recipient = NoticeRecipient.create(mock(StudyMember.class), mock(Notice.class));
        LocalDateTime expectedReadAt = LocalDateTime.of(2026, 8, 24, 12, 30);

        recipient.markAsRead(expectedReadAt);

        assertThat(recipient.getReadAt()).isEqualTo(expectedReadAt);
        assertThat(recipient.isRead()).isTrue();
    }

    @Test
    @DisplayName("공지 수신자의 읽음 시각은 데이터베이스 정밀도인 마이크로초로 기록한다")
    void markAsReadTruncatesToMicrosecondsTest() {
        NoticeRecipient recipient = NoticeRecipient.create(mock(StudyMember.class), mock(Notice.class));
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 12, 30, 0, 123_456_789);

        recipient.markAsRead(now);

        assertThat(recipient.getReadAt()).isEqualTo(now.truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("공지 수신자를 여러 번 읽음 처리해도 최초 읽은 시각을 유지한다")
    void markAsReadIsIdempotentTest() {
        NoticeRecipient recipient = NoticeRecipient.create(mock(StudyMember.class), mock(Notice.class));
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 8, 24, 12, 30);
        LocalDateTime secondReadAt = firstReadAt.plusMinutes(5);

        recipient.markAsRead(firstReadAt);
        recipient.markAsRead(secondReadAt);

        assertThat(recipient.getReadAt()).isEqualTo(firstReadAt);
    }
}
