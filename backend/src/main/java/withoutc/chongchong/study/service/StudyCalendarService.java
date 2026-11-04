package withoutc.chongchong.study.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.SubmissionStatus;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.controller.dto.StudyCalendarEventResponse;
import withoutc.chongchong.study.controller.dto.StudyCalendarResponse;
import withoutc.chongchong.study.entity.StudyCalendarEventType;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyCalendarService {

    private static final Comparator<StudyCalendarEventResponse> EVENT_ORDER = Comparator
            .comparing(StudyCalendarEventResponse::eventAt)
            .thenComparing(StudyCalendarEventResponse::eventType)
            .thenComparing(StudyCalendarEventResponse::resourceId);

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NoticeRepository noticeRepository;
    private final AssignmentRepository assignmentRepository;
    private final Clock clock;

    public StudyCalendarResponse getCalendar(Long userId, Long studyId, int year, int month) {
        studyRepository.getByIdOrThrow(studyId);
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now(clock);

        Stream<StudyCalendarEventResponse> noticeEvents = noticeRepository
                .findCalendarNotices(studyId, member.getId(), startAt, endAt).stream()
                .map(notice -> StudyCalendarEventResponse.notice(
                        notice.id(), notice.title(), notice.createdAt(), notice.readStatus()));
        Stream<StudyCalendarEventResponse> createdAssignmentEvents = assignmentRepository
                .findCalendarCreatedAssignments(studyId, member.getId(), startAt, endAt).stream()
                .map(assignment -> StudyCalendarEventResponse.assignment(StudyCalendarEventType.ASSIGNMENT_CREATED,
                        assignment.id(), assignment.title(), assignment.createdAt(),
                        SubmissionStatus.from(assignment.submittedAt(), assignment.closeAt(), now)));
        Stream<StudyCalendarEventResponse> dueAssignmentEvents = assignmentRepository
                .findCalendarDueAssignments(studyId, member.getId(), startAt, endAt).stream()
                .map(assignment -> StudyCalendarEventResponse.assignment(StudyCalendarEventType.ASSIGNMENT_DUE,
                        assignment.id(), assignment.title(), assignment.closeAt(),
                        SubmissionStatus.from(assignment.submittedAt(), assignment.closeAt(), now)));

        List<StudyCalendarEventResponse> events = Stream.of(noticeEvents, createdAssignmentEvents, dueAssignmentEvents)
                .flatMap(Function.identity())
                .sorted(EVENT_ORDER)
                .toList();
        return new StudyCalendarResponse(year, month, events);
    }
}
