package withoutc.chongchong.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.notice.controller.dto.NoticeReadResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusesResponse;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReadStatus;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.policy.NoticeAccessPolicy;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.NoticeRecipientStatusProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@ExtendWith(MockitoExtension.class)
class NoticeReadServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long STUDY_ID = 10L;
    private static final Long NOTICE_ID = 100L;
    private static final Long MEMBER_ID = 20L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 10, 0);

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeRecipientRepository noticeRecipientRepository;

    @Mock
    private NoticeAccessPolicy noticeAccessPolicy;

    private NoticeReadService noticeReadService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        noticeReadService = new NoticeReadService(
                studyMemberRepository,
                noticeRepository,
                noticeRecipientRepository,
                noticeAccessPolicy,
                clock
        );
    }

    @Test
    @DisplayName("스터디원이 공지를 읽으면 현재 시각을 읽음 시각으로 반환한다")
    void markAsReadTest() {
        StudyMember member = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID);
        NoticeRecipient recipient = recipientOf(notice, null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRepository.getByIdAndStudyIdOrThrow(NOTICE_ID, STUDY_ID)).thenReturn(notice);
        when(noticeRecipientRepository.getByNoticeIdAndMemberIdOrThrow(NOTICE_ID, MEMBER_ID)).thenReturn(recipient);

        NoticeReadResponse response = noticeReadService.markAsRead(USER_ID, STUDY_ID, NOTICE_ID);

        assertThat(response.readAt()).isEqualTo(NOW);
        assertThat(recipient.getReadAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("스터디원이 공지 읽음 상태를 조회하면 읽지 않음과 읽음 상태를 그대로 반환한다")
    void getMyReadStatusTest() {
        StudyMember member = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID);
        NoticeRecipient recipient = recipientOf(notice, null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRepository.getByIdAndStudyIdOrThrow(NOTICE_ID, STUDY_ID)).thenReturn(notice);
        when(noticeRecipientRepository.findByNoticeIdAndMemberId(NOTICE_ID, MEMBER_ID)).thenReturn(Optional.of(recipient));

        NoticeReadStatusResponse unreadResponse = noticeReadService.getMyReadStatus(USER_ID, STUDY_ID, NOTICE_ID);
        ReflectionTestUtils.setField(recipient, "readAt", NOW);
        NoticeReadStatusResponse readResponse = noticeReadService.getMyReadStatus(USER_ID, STUDY_ID, NOTICE_ID);

        assertThat(unreadResponse.readStatus()).isEqualTo(NoticeReadStatus.UNREAD);
        assertThat(unreadResponse.readAt()).isNull();
        assertThat(readResponse.readStatus()).isEqualTo(NoticeReadStatus.READ);
        assertThat(readResponse.readAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("리더가 공지 읽음 현황을 조회하면 읽은 사람과 읽지 않은 사람을 마지막 리마인드 시각과 함께 구분한다")
    void getAllReadStatusesTest() {
        StudyMember leader = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID);
        LocalDateTime remindAt = NOW.plusDays(1);
        LocalDateTime lastRemindAt = NOW.minusHours(1);
        notice.addReminders(List.of(remindAt), NOW);
        List<NoticeRecipientStatusProjection> statuses = List.of(
                new NoticeRecipientStatusProjection(21L, "읽은 멤버", "https://example.com/read.png", true, NOW, null),
                new NoticeRecipientStatusProjection(22L, "리마인드 받은 멤버", "https://example.com/unread.png", false, null,
                        lastRemindAt),
                new NoticeRecipientStatusProjection(23L, "리마인드 없는 멤버", null, false, null, null)
        );
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(noticeRepository.getByIdAndStudyIdOrThrow(NOTICE_ID, STUDY_ID)).thenReturn(notice);
        when(noticeRecipientRepository.findAllReadStatusesByNoticeId(NOTICE_ID)).thenReturn(statuses);

        NoticeReadStatusesResponse response = noticeReadService.getAllReadStatuses(USER_ID, STUDY_ID, NOTICE_ID);

        verify(noticeAccessPolicy).requireCanReadNoticeReadStatuses(leader);
        assertThat(response.id()).isEqualTo(NOTICE_ID);
        assertThat(response.memberCount()).isEqualTo(3);
        assertThat(response.readCount()).isEqualTo(1);
        assertThat(response.remindAt()).isEqualTo(remindAt);
        assertThat(response.readMembers()).containsExactly(
                new NoticeReadStatusesResponse.ReadMember(21L, "읽은 멤버", "https://example.com/read.png", NOW)
        );
        assertThat(response.unreadMembers()).containsExactly(
                new NoticeReadStatusesResponse.UnreadMember(22L, "리마인드 받은 멤버", "https://example.com/unread.png",
                        lastRemindAt),
                new NoticeReadStatusesResponse.UnreadMember(23L, "리마인드 없는 멤버", null, null)
        );
    }

    @Test
    @DisplayName("공지 읽음 현황 조회 정책이 거부하면 저장소 조회를 중단한다")
    void getAllReadStatusesByMemberTest() {
        StudyMember member = mock(StudyMember.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        doThrow(new AuthException(AuthErrorCode.ACCESS_DENIED))
                .when(noticeAccessPolicy).requireCanReadNoticeReadStatuses(member);

        assertThatThrownBy(() -> noticeReadService.getAllReadStatuses(USER_ID, STUDY_ID, NOTICE_ID))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);

        verifyNoInteractions(noticeRepository, noticeRecipientRepository);
    }

    @Test
    @DisplayName("리더도 다른 스터디의 공지 읽음 현황은 조회할 수 없다")
    void getAllReadStatusesFromOtherStudyTest() {
        StudyMember leader = mock(StudyMember.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(noticeRepository.getByIdAndStudyIdOrThrow(NOTICE_ID, STUDY_ID))
                .thenThrow(new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        assertNoticeNotFound(() -> noticeReadService.getAllReadStatuses(USER_ID, STUDY_ID, NOTICE_ID));
        verifyNoInteractions(noticeRecipientRepository);
    }

    @Test
    @DisplayName("수신자가 아니면 내 읽음 상태는 미지정으로 반환한다")
    void getMyReadStatusWhenNotAssignedTest() {
        StudyMember member = mock(StudyMember.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRecipientRepository.findByNoticeIdAndMemberId(NOTICE_ID, MEMBER_ID))
                .thenReturn(Optional.empty());

        NoticeReadStatusResponse response = noticeReadService.getMyReadStatus(USER_ID, STUDY_ID, NOTICE_ID);

        assertThat(response.readStatus()).isEqualTo(NoticeReadStatus.NOT_ASSIGNED);
        assertThat(response.readAt()).isNull();
        verify(noticeRepository).getByIdAndStudyIdOrThrow(NOTICE_ID, STUDY_ID);
    }

    @Test
    @DisplayName("이미 읽은 공지를 다시 읽어도 최초 읽음 시각을 유지한다")
    void markAsReadAgainTest() {
        StudyMember member = mock(StudyMember.class);
        LocalDateTime firstReadAt = NOW.minusDays(1);
        NoticeRecipient recipient = recipientOf(noticeWithId(NOTICE_ID), firstReadAt);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRecipientRepository.getByNoticeIdAndMemberIdOrThrow(NOTICE_ID, MEMBER_ID)).thenReturn(recipient);

        NoticeReadResponse response = noticeReadService.markAsRead(USER_ID, STUDY_ID, NOTICE_ID);

        assertThat(response.readAt()).isEqualTo(firstReadAt);
        assertThat(recipient.getReadAt()).isEqualTo(firstReadAt);
        verify(noticeRepository).getByIdAndStudyIdOrThrow(NOTICE_ID, STUDY_ID);
    }

    private NoticeRecipient recipientOf(Notice notice, LocalDateTime readAt) {
        NoticeRecipient recipient = NoticeRecipient.create(mock(StudyMember.class), notice);
        ReflectionTestUtils.setField(recipient, "readAt", readAt);
        return recipient;
    }

    private Notice noticeWithId(Long noticeId) {
        return noticeWithId(noticeId, STUDY_ID);
    }

    private Notice noticeWithId(Long noticeId, Long studyId) {
        Study study = Study.create("자바 스터디", "설명");
        ReflectionTestUtils.setField(study, "id", studyId);
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");
        ReflectionTestUtils.setField(notice, "id", noticeId);
        return notice;
    }

    private void assertNoticeNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}
