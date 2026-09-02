package withoutc.chongchong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class NoticeRepositoryTest {

    private static final LocalDateTime CLOCK = LocalDateTime.of(2026, 8, 20, 0, 0);

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("스터디 공지를 최신순으로 조회하고 cursor보다 작은 id부터 다음 페이지를 조회한다")
    void findByCursorTest() {
        StudyFixture firstStudy = createStudyFixture("첫 번째 스터디", "첫 번째 리더");
        StudyFixture secondStudy = createStudyFixture("두 번째 스터디", "두 번째 리더");
        List<Notice> notices = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            notices.add(noticeRepository.save(
                    Notice.create(firstStudy.study(), "공지 " + index, "공지 내용")
            ));
        }
        noticeRepository.save(Notice.create(secondStudy.study(), "다른 공지", "공지 내용"));
        noticeRepository.flush();

        List<Notice> firstPage = noticeRepository.findByCursor(
                firstStudy.study().getId(),
                null,
                PageRequest.of(0, 3)
        );
        Long cursor = firstPage.getLast().getId();
        List<Notice> secondPage = noticeRepository.findByCursor(
                firstStudy.study().getId(),
                cursor,
                PageRequest.of(0, 3)
        );

        assertThat(firstPage)
                .extracting(Notice::getId)
                .containsExactly(notices.get(4).getId(), notices.get(3).getId(), notices.get(2).getId());
        assertThat(secondPage)
                .extracting(Notice::getId)
                .containsExactly(notices.get(1).getId(), notices.getFirst().getId());
    }

    @Test
    @DisplayName("멤버별 공지 cursor 조회는 수신 정보가 존재하는 공지만 반환한다")
    void findByCursorAndMemberIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Notice firstMemberNotice = Notice.create(fixture.study(), "첫 번째 멤버 공지", "내용");
        firstMemberNotice.addRecipients(List.of(fixture.firstMember()));
        Notice secondMemberNotice = Notice.create(fixture.study(), "두 번째 멤버 공지", "내용");
        secondMemberNotice.addRecipients(List.of(fixture.secondMember()));
        noticeRepository.saveAllAndFlush(List.of(firstMemberNotice, secondMemberNotice));

        List<Notice> notices = noticeRepository.findByCursorAndMemberId(
                fixture.study().getId(),
                fixture.firstMember().getId(),
                null,
                PageRequest.of(0, 11)
        );

        assertThat(notices)
                .extracting(Notice::getId)
                .containsExactly(firstMemberNotice.getId());
    }

    @Test
    @DisplayName("공지 조회에 성공하면 해당 공지를 반환한다")
    void getByIdOrThrowTest() {
        StudyFixture fixture = createStudyFixture("스터디", "리더");
        Notice notice = noticeRepository.save(
                Notice.create(fixture.study(), "공지", "공지 내용")
        );

        Notice found = noticeRepository.getByIdOrThrow(notice.getId());

        assertThat(found.getId()).isEqualTo(notice.getId());
    }

    @Test
    @DisplayName("공지가 존재하지 않으면 공지 없음 예외를 던진다")
    void getByIdOrThrowNotFoundTest() {
        assertThatThrownBy(() -> noticeRepository.getByIdOrThrow(Long.MAX_VALUE))
                .isInstanceOfSatisfying(NoticeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("리더용 미완료 공지 요약은 읽지 않은 수신자가 있는 공지만 반환하고 읽은 수를 센다")
    void findIncompleteNoticeSummariesByStudyIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Notice incompleteNotice = Notice.create(fixture.study(), "미완료 공지", "내용");
        incompleteNotice.addRecipients(List.of(fixture.firstMember(), fixture.secondMember()));
        Notice completeNotice = Notice.create(fixture.study(), "완료 공지", "내용");
        completeNotice.addRecipients(List.of(fixture.firstMember(), fixture.secondMember()));
        completeNotice.getRecipients().forEach(recipient -> recipient.markAsRead(CLOCK));
        noticeRepository.saveAllAndFlush(List.of(incompleteNotice, completeNotice));

        List<LeaderNoticeSummaryProjection> summaries =
                noticeRepository.findIncompleteNoticeSummariesByStudyId(fixture.study().getId());

        assertThat(summaries)
                .extracting(LeaderNoticeSummaryProjection::id,
                        LeaderNoticeSummaryProjection::title,
                        LeaderNoticeSummaryProjection::memberCount,
                        LeaderNoticeSummaryProjection::completeCount)
                .containsExactly(tuple(incompleteNotice.getId(), "미완료 공지", 2L, 0L));
    }

    @Test
    @DisplayName("멤버용 미완료 공지는 해당 멤버가 읽지 않은 공지만 반환한다")
    void findIncompleteNoticesByStudyIdAndMemberIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Notice unreadNotice = Notice.create(fixture.study(), "읽지 않은 공지", "내용");
        unreadNotice.addRecipients(List.of(fixture.firstMember()));
        Notice readNotice = Notice.create(fixture.study(), "읽은 공지", "내용");
        readNotice.addRecipients(List.of(fixture.firstMember()));
        readNotice.getRecipients().getFirst().markAsRead(CLOCK);
        noticeRepository.saveAllAndFlush(List.of(unreadNotice, readNotice));

        List<Notice> notices = noticeRepository.findIncompleteNoticesByStudyIdAndMemberId(
                fixture.study().getId(), fixture.firstMember().getId());

        assertThat(notices)
                .extracting(Notice::getId)
                .containsExactly(unreadNotice.getId());
    }

    @Test
    @DisplayName("리더용 미완료 공지 개수는 읽지 않은 수신자가 있는 공지만 센다")
    void countIncompleteNoticeByStudyIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Notice incompleteNotice = Notice.create(fixture.study(), "미완료 공지", "내용");
        incompleteNotice.addRecipients(List.of(fixture.firstMember(), fixture.secondMember()));
        Notice completeNotice = Notice.create(fixture.study(), "완료 공지", "내용");
        completeNotice.addRecipients(List.of(fixture.firstMember(), fixture.secondMember()));
        completeNotice.getRecipients().forEach(recipient -> recipient.markAsRead(CLOCK));
        noticeRepository.saveAllAndFlush(List.of(incompleteNotice, completeNotice));

        long count = noticeRepository.countIncompleteNoticeByStudyId(fixture.study().getId());

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("스터디원용 미완료 공지 개수는 해당 멤버가 읽지 않은 공지만 센다")
    void countIncompleteNoticeByStudyIdAndMemberIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Notice notice = Notice.create(fixture.study(), "공지", "내용");
        notice.addRecipients(List.of(fixture.firstMember(), fixture.secondMember()));
        notice.getRecipients().getLast().markAsRead(CLOCK);
        noticeRepository.saveAndFlush(notice);

        long firstMemberCount = noticeRepository.countIncompleteNoticeByStudyIdAndMemberId(
                fixture.study().getId(), fixture.firstMember().getId());
        long secondMemberCount = noticeRepository.countIncompleteNoticeByStudyIdAndMemberId(
                fixture.study().getId(), fixture.secondMember().getId());

        assertThat(firstMemberCount).isEqualTo(1L);
        assertThat(secondMemberCount).isZero();
    }

    private StudyFixture createStudyFixture(String studyName, String userName) {
        User user = userRepository.save(User.create(userName, null));
        Study study = studyRepository.save(Study.create(studyName, "설명"));
        StudyMember leader = studyMemberRepository.save(
                StudyMember.create(study, user, userName, null, StudyMemberRole.LEADER)
        );
        return new StudyFixture(study, leader);
    }

    private record StudyFixture(Study study, StudyMember leader) {
    }

    private StudyWithMembersFixture createStudyWithMembersFixture() {
        User leaderUser = userRepository.save(User.create("리더", null));
        User firstMemberUser = userRepository.save(User.create("첫 번째 멤버", null));
        User secondMemberUser = userRepository.save(User.create("두 번째 멤버", null));
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember leader = studyMemberRepository.save(
                StudyMember.create(study, leaderUser, leaderUser.getName(), null, StudyMemberRole.LEADER)
        );
        StudyMember firstMember = studyMemberRepository.save(
                StudyMember.create(study, firstMemberUser, firstMemberUser.getName(), null, StudyMemberRole.MEMBER)
        );
        StudyMember secondMember = studyMemberRepository.save(
                StudyMember.create(study, secondMemberUser, secondMemberUser.getName(), null, StudyMemberRole.MEMBER)
        );
        return new StudyWithMembersFixture(study, leader, firstMember, secondMember);
    }

    private record StudyWithMembersFixture(
            Study study,
            StudyMember leader,
            StudyMember firstMember,
            StudyMember secondMember
    ) {
    }
}
