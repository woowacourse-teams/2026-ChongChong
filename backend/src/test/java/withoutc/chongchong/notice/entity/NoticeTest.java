package withoutc.chongchong.notice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

class NoticeTest {

    private final Study study = mock(Study.class);

    @Test
    @DisplayName("공지 생성 시 제목과 내용을 검증한다")
    void createWithInvalidTitleAndContentTest() {
        assertThatThrownBy(() -> Notice.create(study, " ", "공지 내용"))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_TITLE);

        assertThatThrownBy(() -> Notice.create(study, "공지 제목", " "))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_CONTENT);
    }

    @Test
    @DisplayName("공지 제목은 20자까지 허용하고 20자를 초과하면 거부한다")
    void validateTitleLengthTest() {
        String maxLengthTitle = "가".repeat(20);
        String overLengthTitle = "가".repeat(21);

        assertThatCode(() -> Notice.create(study, maxLengthTitle, "공지 내용"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> Notice.create(study, overLengthTitle, "공지 내용"))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_TITLE);
    }

    @Test
    @DisplayName("공지 내용은 10000자까지 허용하고 10000자를 초과하면 거부한다")
    void validateContentLengthTest() {
        String maxLengthContent = "가".repeat(10000);
        String overLengthContent = "가".repeat(10001);

        assertThatCode(() -> Notice.create(study, "공지 제목", maxLengthContent))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> Notice.create(study, "공지 제목", overLengthContent))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_CONTENT);
    }

    @Test
    @DisplayName("같은 member id의 공지 수신자는 한 번만 추가한다")
    void addDistinctRecipientsByMemberIdTest() {
        StudyMember firstMember = mock(StudyMember.class);
        StudyMember duplicateMember = mock(StudyMember.class);
        when(firstMember.getId()).thenReturn(1L);
        when(duplicateMember.getId()).thenReturn(1L);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");

        notice.addRecipients(List.of(firstMember, duplicateMember));
        notice.addRecipients(List.of(duplicateMember));

        assertThat(notice.getRecipients())
                .singleElement()
                .extracting(NoticeRecipient::getMember)
                .isSameAs(firstMember);
    }

    @Test
    @DisplayName("member id가 없는 스터디원은 공지 수신자에서 제외한다")
    void excludeRecipientWithoutMemberIdTest() {
        StudyMember unsavedMember = mock(StudyMember.class);
        StudyMember savedMember = mock(StudyMember.class);
        when(unsavedMember.getId()).thenReturn(null);
        when(savedMember.getId()).thenReturn(1L);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");

        notice.addRecipients(List.of(unsavedMember, savedMember));

        assertThat(notice.getRecipients())
                .singleElement()
                .extracting(NoticeRecipient::getMember)
                .isSameAs(savedMember);
    }

    @Test
    @DisplayName("동일한 리마인드 시각은 한 번만 추가한다")
    void addDistinctRemindersTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime remindAt = now.plusHours(1);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");

        notice.addReminders(List.of(remindAt, remindAt), now);

        assertThat(notice.getReminders()).hasSize(1);
    }

    @Test
    @DisplayName("리마인드 시각을 설정하지 않으면 리마인더를 추가하지 않는다")
    void addNullRemindersTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");

        notice.addReminders(null, now);

        assertThat(notice.getReminders()).isEmpty();
    }

    @Test
    @DisplayName("null 또는 미래가 아닌 시각의 리마인더는 추가할 수 없다")
    void addNonFutureReminderTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");

        assertInvalidRemindAt(notice, Collections.singletonList(null), now);
        assertInvalidRemindAt(notice, List.of(now.minusNanos(1)), now);
        assertInvalidRemindAt(notice, List.of(now), now);
    }

    @Test
    @DisplayName("공지 수정 시 공백 제목과 내용을 거부한다")
    void updateWithBlankTitleAndContentTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");

        assertThatThrownBy(() -> notice.update(" ", null, null, now))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_TITLE);
        assertThatThrownBy(() -> notice.update(null, " ", null, now))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_CONTENT);
    }

    @Test
    @DisplayName("공지 수정 시 제목과 내용의 최대 길이 경계를 검증한다")
    void updateWithLengthBoundaryTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        String maxLengthTitle = "가".repeat(20);
        String overLengthTitle = "가".repeat(21);
        String maxLengthContent = "가".repeat(10000);
        String overLengthContent = "가".repeat(10001);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");

        assertThatCode(() -> notice.update(maxLengthTitle, maxLengthContent, null, now))
                .doesNotThrowAnyException();
        assertThat(notice.getTitle()).isEqualTo(maxLengthTitle);
        assertThat(notice.getContent()).isEqualTo(maxLengthContent);
        assertThatThrownBy(() -> notice.update(overLengthTitle, null, null, now))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_TITLE);
        assertThatThrownBy(() -> notice.update(null, overLengthContent, null, now))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_CONTENT);
    }

    @Test
    @DisplayName("교체할 리마인드 시각 중 잘못된 값이 있으면 기존 리마인더를 유지한다")
    void updateWithPartiallyInvalidRemindersTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime existingRemindAt = now.plusHours(1);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addReminders(List.of(existingRemindAt), now);

        assertThatThrownBy(() -> notice.update(null, null, List.of(now.plusHours(2), now), now))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_REMIND_AT);
        assertThat(notice.getReminders())
                .extracting(NoticeReminder::getRemindAt)
                .containsExactly(existingRemindAt);
    }

    @Test
    @DisplayName("교체할 리마인드 시각에 null이 있으면 기존 리마인더를 유지한다")
    void updateWithNullReminderTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime existingRemindAt = now.plusHours(1);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addReminders(List.of(existingRemindAt), now);

        assertThatThrownBy(() -> notice.update(
                null,
                null,
                Collections.singletonList(null),
                now
        ))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_REMIND_AT);
        assertThat(notice.getReminders())
                .extracting(NoticeReminder::getRemindAt)
                .containsExactly(existingRemindAt);
    }

    @Test
    @DisplayName("빈 리마인드 목록으로 수정하면 발송 완료 리마인더만 유지한다")
    void updateWithEmptyRemindersTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime sentRemindAt = now.plusHours(1);
        LocalDateTime pendingRemindAt = now.plusHours(2);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addReminders(List.of(sentRemindAt, pendingRemindAt), now);
        notice.getReminders().getFirst().markAsSent();

        notice.update(null, null, List.of(), now);

        assertThat(notice.getReminders())
                .extracting(NoticeReminder::getRemindAt)
                .containsExactly(sentRemindAt);
        assertThat(notice.getNextRemindAt()).isNull();
    }

    @Test
    @DisplayName("중복된 리마인드 시각으로 수정하면 리마인더를 한 번만 등록한다")
    void updateWithDuplicateRemindersTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime newRemindAt = now.plusHours(1);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");

        notice.update(null, null, List.of(newRemindAt, newRemindAt), now);

        assertThat(notice.getReminders())
                .extracting(NoticeReminder::getRemindAt)
                .containsExactly(newRemindAt);
    }

    @Test
    @DisplayName("공지 수정 시 발송·실패 리마인더는 유지하고 대기 리마인더만 교체한다")
    void updateTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime sentRemindAt = now.plusHours(1);
        LocalDateTime failedRemindAt = now.plusHours(2);
        LocalDateTime pendingRemindAt = now.plusHours(3);
        LocalDateTime newRemindAt = now.plusHours(4);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addReminders(List.of(sentRemindAt, failedRemindAt, pendingRemindAt), now);
        notice.getReminders().getFirst().markAsSent();
        ReflectionTestUtils.setField(
                notice.getReminders().get(1),
                "status",
                NoticeReminderStatus.FAILED
        );

        notice.update("수정 제목", null, List.of(newRemindAt), now);

        assertThat(notice.getTitle()).isEqualTo("수정 제목");
        assertThat(notice.getContent()).isEqualTo("기존 내용");
        assertThat(notice.getReminders())
                .extracting(NoticeReminder::getRemindAt)
                .containsExactlyInAnyOrder(sentRemindAt, failedRemindAt, newRemindAt);
        assertThat(notice.getNextRemindAt()).isEqualTo(newRemindAt);
    }

    @Test
    @DisplayName("발송하지 않은 리마인더 중 가장 빠른 예정 시각을 반환한다")
    void getNextRemindAtTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime firstRemindAt = now.plusHours(1);
        LocalDateTime nextRemindAt = now.plusHours(2);
        LocalDateTime lastRemindAt = now.plusHours(3);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");
        notice.addReminders(List.of(lastRemindAt, firstRemindAt, nextRemindAt), now);
        notice.getReminders().get(1).markAsSent();

        LocalDateTime result = notice.getNextRemindAt();

        assertThat(result).isEqualTo(nextRemindAt);
    }

    @Test
    @DisplayName("발송할 리마인더가 없으면 예정 시각으로 null을 반환한다")
    void getNextRemindAtWithoutPendingReminderTest() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 10, 0);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");
        notice.addReminders(List.of(now.plusHours(1)), now);
        notice.getReminders().getFirst().markAsSent();

        LocalDateTime result = notice.getNextRemindAt();

        assertThat(result).isNull();
    }

    private void assertInvalidRemindAt(Notice notice, List<LocalDateTime> remindAts, LocalDateTime now) {
        assertThatThrownBy(() -> notice.addReminders(remindAts, now))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_REMIND_AT);
    }
}
