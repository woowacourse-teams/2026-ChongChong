package withoutc.chongchong.notice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
}
