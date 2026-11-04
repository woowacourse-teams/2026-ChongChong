package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.assignment.entity.SubmissionStatus;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentCalendarProjection;
import withoutc.chongchong.notice.entity.NoticeReadStatus;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.NoticeCalendarProjection;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.controller.dto.StudyCalendarEventResponse;
import withoutc.chongchong.study.entity.StudyCalendarEventType;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;

@ExtendWith(MockitoExtension.class)
class StudyCalendarServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyMemberRepository memberRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private AssignmentRepository assignmentRepository;

    private StudyCalendarService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new StudyCalendarService(studyRepository, memberRepository, noticeRepository, assignmentRepository,
                clock);
    }

    @ParameterizedTest
    @MethodSource("months")
    @DisplayName("윤년·연말과 지원 연도 경계에서도 다음 달 시작을 배타적 상한으로 사용한다")
    void usesMonthBoundaries(int year, int month, LocalDateTime startAt, LocalDateTime endAt) {
        allowMember();

        var response = service.getCalendar(1L, 2L, year, month);

        assertThat(response.year()).isEqualTo(year);
        assertThat(response.month()).isEqualTo(month);
        assertThat(response.events()).isEmpty();
        verify(noticeRepository).findCalendarNotices(2L, 3L, startAt, endAt);
        verify(assignmentRepository).findCalendarCreatedAssignments(2L, 3L, startAt, endAt);
        verify(assignmentRepository).findCalendarDueAssignments(2L, 3L, startAt, endAt);
    }

    @Test
    @DisplayName("같은 시각에는 이벤트 종류와 원본 ID로 정렬하고 같은 과제 생성·마감을 모두 반환한다")
    void ordersEventsAndKeepsCreationAndDeadline() {
        allowMember();
        when(noticeRepository.findCalendarNotices(2L, 3L, START, END)).thenReturn(List.of(
                new NoticeCalendarProjection(5L, "읽은 공지", NOW, 7L, NOW),
                new NoticeCalendarProjection(4L, "이전 공지", NOW, null, null)));
        var assignment = new AssignmentCalendarProjection(1L, "같은 날 과제", NOW, NOW, NOW);
        when(assignmentRepository.findCalendarCreatedAssignments(2L, 3L, START, END))
                .thenReturn(List.of(assignment));
        when(assignmentRepository.findCalendarDueAssignments(2L, 3L, START, END)).thenReturn(List.of(assignment));

        var events = service.getCalendar(1L, 2L, 2026, 11).events();

        assertThat(events).extracting(StudyCalendarEventResponse::eventType, StudyCalendarEventResponse::resourceId)
                .containsExactly(tuple(StudyCalendarEventType.NOTICE_CREATED, 4L),
                        tuple(StudyCalendarEventType.NOTICE_CREATED, 5L),
                        tuple(StudyCalendarEventType.ASSIGNMENT_CREATED, 1L),
                        tuple(StudyCalendarEventType.ASSIGNMENT_DUE, 1L));
        assertThat(events.getFirst().readStatus()).isEqualTo(NoticeReadStatus.NOT_ASSIGNED);
        assertThat(events.get(1).readStatus()).isEqualTo(NoticeReadStatus.READ);
        assertThat(events.get(2).submissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(events.getLast().submissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("종류나 원본 ID보다 실제 이벤트 시각을 먼저 정렬하고 마감에는 closeAt을 사용한다")
    void ordersByEventTimeBeforeTypeAndId() {
        allowMember();
        when(noticeRepository.findCalendarNotices(2L, 3L, START, END)).thenReturn(List.of(
                new NoticeCalendarProjection(1L, "공지", NOW.plusMinutes(1), null, null)));
        when(assignmentRepository.findCalendarCreatedAssignments(2L, 3L, START, END)).thenReturn(List.of(
                new AssignmentCalendarProjection(9L, "이번 달 생성", START, END, null)));
        when(assignmentRepository.findCalendarDueAssignments(2L, 3L, START, END)).thenReturn(List.of(
                new AssignmentCalendarProjection(100L, "지난달 생성", START.minusMonths(1), NOW, null)));

        var events = service.getCalendar(1L, 2L, 2026, 11).events();

        assertThat(events).extracting(StudyCalendarEventResponse::eventType, StudyCalendarEventResponse::resourceId,
                        StudyCalendarEventResponse::eventAt, StudyCalendarEventResponse::resourceType)
                .containsExactly(
                        tuple(StudyCalendarEventType.ASSIGNMENT_CREATED, 9L, START, ResourceType.ASSIGNMENT),
                        tuple(StudyCalendarEventType.ASSIGNMENT_DUE, 100L, NOW, ResourceType.ASSIGNMENT),
                        tuple(StudyCalendarEventType.NOTICE_CREATED, 1L, NOW.plusMinutes(1), ResourceType.NOTICE));
        assertThat(events.get(1).title()).isEqualTo("지난달 생성");
    }

    @Test
    @DisplayName("공지의 미대상·미읽음·읽음 상태를 그대로 반환하고 과제 상태와 섞지 않는다")
    void mapsPersonalNoticeStatuses() {
        allowMember();
        when(noticeRepository.findCalendarNotices(2L, 3L, START, END)).thenReturn(List.of(
                new NoticeCalendarProjection(1L, "미대상", START, null, null),
                new NoticeCalendarProjection(2L, "미읽음", START, 20L, null),
                new NoticeCalendarProjection(3L, "읽음", START, 30L, NOW)));

        var events = service.getCalendar(1L, 2L, 2026, 11).events();

        assertThat(events).extracting(StudyCalendarEventResponse::readStatus)
                .containsExactly(NoticeReadStatus.NOT_ASSIGNED, NoticeReadStatus.UNREAD, NoticeReadStatus.READ);
        assertThat(events).allSatisfy(event -> assertThat(event.submissionStatus()).isNull());
    }

    @Test
    @DisplayName("조회 도중 시간이 마감을 지나도 한 번 읽은 한국 기준 시각으로 모든 이벤트 상태를 판정한다")
    void capturesCurrentTimeOncePerRequest() {
        allowMember();
        Clock clock = mock(Clock.class);
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        Instant instant = Instant.parse("2026-11-04T01:00:00Z");
        when(clock.instant()).thenReturn(instant, instant.plusSeconds(1));
        var serviceWithClock = new StudyCalendarService(studyRepository, memberRepository, noticeRepository,
                assignmentRepository, clock);
        var assignment = new AssignmentCalendarProjection(1L, "마감 경계", START, NOW, null);
        when(assignmentRepository.findCalendarCreatedAssignments(2L, 3L, START, END)).thenReturn(List.of(assignment));
        when(assignmentRepository.findCalendarDueAssignments(2L, 3L, START, END)).thenReturn(List.of(assignment));

        var events = serviceWithClock.getCalendar(1L, 2L, 2026, 11).events();

        assertThat(events).hasSize(2)
                .allSatisfy(event -> assertThat(event.submissionStatus()).isEqualTo(SubmissionStatus.NOT_SUBMITTED));
        verify(clock).instant();
    }

    @ParameterizedTest
    @MethodSource("submissionStates")
    @DisplayName("한국 기준 현재 시각과 최초 제출·마감 시각으로 본인 제출 상태를 구분한다")
    void determinesSubmissionStatus(LocalDateTime submittedAt, LocalDateTime closeAt, SubmissionStatus expected) {
        allowMember();
        when(assignmentRepository.findCalendarCreatedAssignments(2L, 3L, START, END)).thenReturn(List.of(
                new AssignmentCalendarProjection(1L, "과제", START, closeAt, submittedAt)));

        var event = service.getCalendar(1L, 2L, 2026, 11).events().getFirst();

        assertThat(event.submissionStatus()).isEqualTo(expected);
        assertThat(event.readStatus()).isNull();
    }

    @Test
    @DisplayName("스터디가 없으면 멤버나 이벤트를 조회하지 않는다")
    void rejectsMissingStudyBeforeLookingUpEvents() {
        when(studyRepository.getByIdOrThrow(2L)).thenThrow(new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        assertThatThrownBy(() -> service.getCalendar(1L, 2L, 2026, 11))
                .isInstanceOf(StudyException.class);

        verifyNoInteractions(memberRepository, noticeRepository, assignmentRepository);
    }

    @Test
    @DisplayName("현재 멤버가 아니면 공지·과제 데이터를 조회하지 않는다")
    void rejectsNonMemberBeforeLookingUpEvents() {
        when(memberRepository.getByStudyIdAndUserIdOrThrow(2L, 1L))
                .thenThrow(new StudyMemberException(StudyMemberErrorCode.STUDY_ACCESS_DENIED));

        assertThatThrownBy(() -> service.getCalendar(1L, 2L, 2026, 11))
                .isInstanceOf(StudyMemberException.class);

        verifyNoInteractions(noticeRepository, assignmentRepository);
    }

    private void allowMember() {
        StudyMember member = mock(StudyMember.class);
        when(member.getId()).thenReturn(3L);
        when(memberRepository.getByStudyIdAndUserIdOrThrow(2L, 1L)).thenReturn(member);
    }

    private static Stream<Arguments> months() {
        return Stream.of(
                Arguments.of(2024, 2, LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)),
                Arguments.of(2025, 2, LocalDateTime.of(2025, 2, 1, 0, 0), LocalDateTime.of(2025, 3, 1, 0, 0)),
                Arguments.of(2026, 12, LocalDateTime.of(2026, 12, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0)),
                Arguments.of(1, 1, LocalDateTime.of(1, 1, 1, 0, 0), LocalDateTime.of(1, 2, 1, 0, 0)),
                Arguments.of(9998, 12, LocalDateTime.of(9998, 12, 1, 0, 0), LocalDateTime.of(9999, 1, 1, 0, 0)));
    }

    private static Stream<Arguments> submissionStates() {
        return Stream.of(
                Arguments.of(null, NOW.plusNanos(1), SubmissionStatus.NOT_SUBMITTED),
                Arguments.of(null, NOW, SubmissionStatus.NOT_SUBMITTED),
                Arguments.of(null, NOW.minusNanos(1), SubmissionStatus.MISSING),
                Arguments.of(NOW.minusSeconds(1), NOW, SubmissionStatus.SUBMITTED),
                Arguments.of(NOW, NOW, SubmissionStatus.SUBMITTED),
                Arguments.of(NOW.plusNanos(1), NOW, SubmissionStatus.LATE_SUBMITTED));
    }
}
