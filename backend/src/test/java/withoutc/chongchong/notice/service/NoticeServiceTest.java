package withoutc.chongchong.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.controller.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.controller.dto.NoticeDetailResponse;
import withoutc.chongchong.notice.controller.dto.NoticeListResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadResponse;
import withoutc.chongchong.notice.controller.dto.NoticeReadStatusResponse;
import withoutc.chongchong.notice.controller.dto.NoticeStatusesResponse;
import withoutc.chongchong.notice.controller.dto.NoticeSummaryResponse;
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.NoticeReadStatusProjection;
import withoutc.chongchong.notice.repository.projection.NoticeRecipientStatusProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.user.entity.User;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

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

    private NoticeService noticeService;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"), zoneId);
        noticeService = new NoticeService(
                studyMemberRepository,
                noticeRepository,
                noticeRecipientRepository,
                clock
        );
    }

    @Test
    @DisplayName("리더가 공지를 생성하면 리더를 제외한 스터디원이 수신자로 등록된다")
    void createTest() {
        Study study = mock(Study.class);
        StudyMember leader = mock(StudyMember.class);
        StudyMember member = mock(StudyMember.class);
        LocalDateTime remindAt = NOW.plusDays(1);
        NoticeCreateRequest request = new NoticeCreateRequest("공지 제목", "공지 내용", List.of(remindAt));
        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(leader.getStudy()).thenReturn(study);
        when(studyMemberRepository.findAllByStudyId(STUDY_ID)).thenReturn(List.of(leader, member));
        when(member.isLeader()).thenReturn(false);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            ReflectionTestUtils.setField(notice, "id", NOTICE_ID);
            return notice;
        });

        NoticeCreateResponse response = noticeService.create(USER_ID, STUDY_ID, request);

        verify(noticeRepository).save(noticeCaptor.capture());
        Notice notice = noticeCaptor.getValue();
        assertThat(response.noticeId()).isEqualTo(NOTICE_ID);
        assertThat(notice.getStudy()).isSameAs(study);
        assertThat(notice.getWriter()).isSameAs(leader);
        assertThat(notice.getRecipientCount()).isEqualTo(1);
        assertThat(notice.getRecipients().getFirst().getMember()).isSameAs(member);
        assertThat(notice.getNextRemindAt()).isEqualTo(remindAt);
    }

    @Test
    @DisplayName("리더가 아니면 공지를 수정할 수 없다")
    void updateByMemberTest() {
        StudyMember member = mock(StudyMember.class);
        NoticeUpdateRequest request = new NoticeUpdateRequest("수정 제목", null, null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.isLeader()).thenReturn(false);

        assertThatThrownBy(() -> noticeService.update(USER_ID, STUDY_ID, NOTICE_ID, request))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);

        verifyNoInteractions(noticeRepository);
    }

    @Test
    @DisplayName("공지 수정 시 제목과 대기 리마인더를 변경한다")
    void updateTest() {
        Study study = mock(Study.class);
        StudyMember leader = mock(StudyMember.class);
        when(leader.getStudy()).thenReturn(study);
        Notice notice = Notice.create(leader, "기존 제목", "기존 내용");
        LocalDateTime oldRemindAt = NOW.plusHours(1);
        LocalDateTime newRemindAt = NOW.plusHours(2);
        notice.addReminders(List.of(oldRemindAt), NOW);
        NoticeUpdateRequest request = new NoticeUpdateRequest("수정 제목", null, List.of(newRemindAt));
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);
        when(study.getId()).thenReturn(STUDY_ID);

        noticeService.update(USER_ID, STUDY_ID, NOTICE_ID, request);

        assertThat(notice.getTitle()).isEqualTo("수정 제목");
        assertThat(notice.getContent()).isEqualTo("기존 내용");
        assertThat(notice.getNextRemindAt()).isEqualTo(newRemindAt);
        verify(noticeRepository).save(notice);
    }

    @Test
    @DisplayName("공지 삭제 시 애그리거트 루트를 삭제한다")
    void deleteTest() {
        Study study = mock(Study.class);
        StudyMember leader = mock(StudyMember.class);
        when(leader.getStudy()).thenReturn(study);
        Notice notice = Notice.create(leader, "공지 제목", "공지 내용");
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);
        when(study.getId()).thenReturn(STUDY_ID);

        noticeService.delete(USER_ID, STUDY_ID, NOTICE_ID);

        verify(noticeRepository).delete(notice);
    }

    @Test
    @DisplayName("리더가 공지 목록을 조회하면 size만큼 반환하고 다음 cursor를 계산한다")
    void getListForLeaderTest() {
        StudyMember leader = mock(StudyMember.class);
        Notice firstNotice = noticeWithId(300L);
        Notice secondNotice = noticeWithId(200L);
        Notice nextPageNotice = noticeWithId(100L);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(noticeRepository.findByCursor(STUDY_ID, null, PageRequest.of(0, 3)))
                .thenReturn(List.of(firstNotice, secondNotice, nextPageNotice));

        NoticeListResponse response = noticeService.getList(USER_ID, STUDY_ID, null, 2);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(200L);
        assertThat(response.notices())
                .extracting(NoticeSummaryResponse::id)
                .containsExactly(300L, 200L);
        assertThat(response.notices().getFirst().recipientCount()).isZero();
        assertThat(response.notices().getFirst().readRecipientCount()).isZero();
        assertThat(response.notices().getFirst().remindAt()).isNull();
        assertThat(response.notices().getFirst().isComplete()).isTrue();
        verify(noticeRepository).findByCursor(
                STUDY_ID,
                null,
                PageRequest.of(0, 3)
        );
        verifyNoInteractions(noticeRecipientRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {101, Integer.MAX_VALUE})
    @DisplayName("페이지 크기가 최대값을 초과하면 저장소를 조회하지 않는다")
    void rejectPageSizeOverMaximumTest(int size) {
        assertThatThrownBy(() -> noticeService.getList(USER_ID, STUDY_ID, null, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("페이지 크기는 1 이상 100 이하여야 합니다.");

        verifyNoInteractions(studyMemberRepository, noticeRepository, noticeRecipientRepository);
    }

    @Test
    @DisplayName("스터디원이 공지 목록을 조회하면 읽음 상태를 한 번에 조회한다")
    void getListForMemberTest() {
        StudyMember member = mock(StudyMember.class);
        NoticeReadStatusProjection readStatus = readStatus(NOTICE_ID, NOW);
        NoticeReadStatusProjection unreadStatus = readStatus(200L, null);
        Notice firstNotice = noticeWithId(NOTICE_ID);
        Notice secondNotice = noticeWithId(200L);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRepository.findByCursor(
                STUDY_ID,
                null,
                PageRequest.of(0, 11)
        )).thenReturn(List.of(firstNotice, secondNotice));
        when(noticeRecipientRepository.findMyReadStatusesByNoticeIdsAndMemberId(
                List.of(NOTICE_ID, 200L),
                MEMBER_ID
        )).thenReturn(List.of(readStatus, unreadStatus));

        NoticeListResponse response = noticeService.getList(USER_ID, STUDY_ID, null, 10);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.notices()).hasSize(2);
        assertThat(response.notices().getFirst().recipientCount()).isNull();
        assertThat(response.notices().getFirst().readRecipientCount()).isNull();
        assertThat(response.notices().getFirst().remindAt()).isNull();
        assertThat(response.notices().getFirst().isComplete()).isTrue();
        assertThat(response.notices().getLast().isComplete()).isFalse();
        verify(noticeRecipientRepository).findMyReadStatusesByNoticeIdsAndMemberId(
                List.of(NOTICE_ID, 200L),
                MEMBER_ID
        );
    }

    @Test
    @DisplayName("스터디원의 공지 수신자 정보가 없으면 목록 조회를 거부한다")
    void getListWithoutRecipientTest() {
        StudyMember member = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRepository.findByCursor(
                STUDY_ID,
                null,
                PageRequest.of(0, 11)
        )).thenReturn(List.of(notice));
        when(noticeRecipientRepository.findMyReadStatusesByNoticeIdsAndMemberId(
                List.of(NOTICE_ID),
                MEMBER_ID
        )).thenReturn(List.of());

        assertThatThrownBy(() -> noticeService.getList(USER_ID, STUDY_ID, null, 10))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.NOTICE_RECIPIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("스터디 참여자가 소속 스터디의 공지 상세 정보를 조회한다")
    void getDetailTest() {
        StudyMember member = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);

        NoticeDetailResponse response = noticeService.getDetail(USER_ID, STUDY_ID, NOTICE_ID);

        assertThat(response.id()).isEqualTo(NOTICE_ID);
        assertThat(response.title()).isEqualTo("공지 제목");
        assertThat(response.content()).isEqualTo("공지 내용");
        assertThat(response.writer()).isEqualTo("리더");
    }

    @Test
    @DisplayName("스터디에 참여하지 않은 사용자는 공지 상세 정보를 조회할 수 없다")
    void getDetailByNonParticipantTest() {
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID))
                .thenThrow(new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));

        assertThatThrownBy(() -> noticeService.getDetail(USER_ID, STUDY_ID, NOTICE_ID))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_ACCESS_DENIED);
        verifyNoInteractions(noticeRepository);
    }

    @Test
    @DisplayName("다른 스터디의 공지 상세 정보는 조회할 수 없다")
    void getDetailFromOtherStudyTest() {
        StudyMember member = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID, 999L);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);

        assertNoticeNotFound(() -> noticeService.getDetail(USER_ID, STUDY_ID, NOTICE_ID));
    }

    @Test
    @DisplayName("다른 스터디의 공지는 수정할 수 없다")
    void updateNoticeFromOtherStudyTest() {
        StudyMember leader = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID, 999L);
        NoticeUpdateRequest request = new NoticeUpdateRequest("수정 제목", null, null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);

        assertNoticeNotFound(() -> noticeService.update(USER_ID, STUDY_ID, NOTICE_ID, request));
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    @DisplayName("다른 스터디의 공지는 삭제할 수 없다")
    void deleteNoticeFromOtherStudyTest() {
        StudyMember leader = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID, 999L);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);

        assertNoticeNotFound(() -> noticeService.delete(USER_ID, STUDY_ID, NOTICE_ID));
        verify(noticeRepository, never()).delete(any(Notice.class));
    }

    @Test
    @DisplayName("스터디원이 공지를 읽으면 현재 시각을 읽음 시각으로 반환한다")
    void markAsReadTest() {
        StudyMember member = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID);
        NoticeRecipient recipient = recipientOf(notice, null);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.getId()).thenReturn(MEMBER_ID);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);
        when(noticeRecipientRepository.getByNoticeIdAndMemberIdOrThrow(NOTICE_ID, MEMBER_ID)).thenReturn(recipient);

        NoticeReadResponse response = noticeService.markAsRead(USER_ID, STUDY_ID, NOTICE_ID);

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
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);
        when(noticeRecipientRepository.getByNoticeIdAndMemberIdOrThrow(NOTICE_ID, MEMBER_ID)).thenReturn(recipient);

        NoticeReadStatusResponse unreadResponse = noticeService.getMyReadStatus(USER_ID, STUDY_ID, NOTICE_ID);
        ReflectionTestUtils.setField(recipient, "readAt", NOW);
        NoticeReadStatusResponse readResponse = noticeService.getMyReadStatus(USER_ID, STUDY_ID, NOTICE_ID);

        assertThat(unreadResponse.isRead()).isFalse();
        assertThat(unreadResponse.readAt()).isNull();
        assertThat(readResponse.isRead()).isTrue();
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
                new NoticeRecipientStatusProjection(21L, "읽은 멤버", "https://example.com/read.png", true, null),
                new NoticeRecipientStatusProjection(22L, "리마인드 받은 멤버", "https://example.com/unread.png", false,
                        lastRemindAt),
                new NoticeRecipientStatusProjection(23L, "리마인드 없는 멤버", null, false, null)
        );
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);
        when(noticeRecipientRepository.findAllReadStatusesByNoticeId(NOTICE_ID)).thenReturn(statuses);

        NoticeStatusesResponse response = noticeService.getAllReadStatuses(USER_ID, STUDY_ID, NOTICE_ID);

        assertThat(response.id()).isEqualTo(NOTICE_ID);
        assertThat(response.memberCount()).isEqualTo(3);
        assertThat(response.readCount()).isEqualTo(1);
        assertThat(response.remindAt()).isEqualTo(remindAt);
        assertThat(response.readMembers()).containsExactly(
                new NoticeStatusesResponse.ReadMember(21L, "읽은 멤버", "https://example.com/read.png")
        );
        assertThat(response.unreadMembers()).containsExactly(
                new NoticeStatusesResponse.UnreadMember(22L, "리마인드 받은 멤버", "https://example.com/unread.png",
                        lastRemindAt),
                new NoticeStatusesResponse.UnreadMember(23L, "리마인드 없는 멤버", null, null)
        );
    }

    @Test
    @DisplayName("리더가 아닌 스터디원은 공지 읽음 현황을 조회할 수 없다")
    void getAllReadStatusesByMemberTest() {
        StudyMember member = mock(StudyMember.class);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(member);
        when(member.isLeader()).thenReturn(false);

        assertThatThrownBy(() -> noticeService.getAllReadStatuses(USER_ID, STUDY_ID, NOTICE_ID))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);

        verifyNoInteractions(noticeRepository, noticeRecipientRepository);
    }

    @Test
    @DisplayName("리더도 다른 스터디의 공지 읽음 현황은 조회할 수 없다")
    void getAllReadStatusesFromOtherStudyTest() {
        StudyMember leader = mock(StudyMember.class);
        Notice notice = noticeWithId(NOTICE_ID, 999L);
        when(studyMemberRepository.getByStudyIdAndUserIdOrThrow(STUDY_ID, USER_ID)).thenReturn(leader);
        when(leader.isLeader()).thenReturn(true);
        when(noticeRepository.getByIdOrThrow(NOTICE_ID)).thenReturn(notice);

        assertNoticeNotFound(() -> noticeService.getAllReadStatuses(USER_ID, STUDY_ID, NOTICE_ID));
        verifyNoInteractions(noticeRecipientRepository);
    }

    private NoticeReadStatusProjection readStatus(Long noticeId, LocalDateTime readAt) {
        return new NoticeReadStatusProjection(noticeId, readAt);
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
        User user = User.create("리더", null);
        StudyMember writer = StudyMember.create(study, user, "리더", null, StudyMemberRole.LEADER);
        ReflectionTestUtils.setField(study, "id", studyId);
        Notice notice = Notice.create(writer, "공지 제목", "공지 내용");
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
