package withoutc.chongchong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReadStatus;
import withoutc.chongchong.notice.repository.projection.NoticeCalendarProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoticeCalendarRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2024, 2, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2024, 3, 1, 0, 0);

    @Autowired
    private NoticeRepository notices;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private StudyMemberRepository members;
    @Autowired
    private UserRepository users;
    @Autowired
    private JdbcTemplate jdbc;

    private Study study;
    private StudyMember member;
    private StudyMember other;

    @BeforeEach
    void setUp() {
        study = studies.saveAndFlush(Study.create("조회 스터디", "설명", false));
        member = saveMember("조회 멤버");
        other = saveMember("다른 멤버");
    }

    @Test
    @DisplayName("월 시작은 포함하고 다음 달 시작과 다른 스터디 공지는 제외하며 월말 소수 초를 보존한다")
    void respectsStudyAndMonthBoundaries() {
        Notice first = saveNotice(study, START, List.of());
        Notice last = saveNotice(study, END.minusNanos(1000), List.of());
        saveNotice(study, START.minusNanos(1000), List.of());
        saveNotice(study, END, List.of());
        Study anotherStudy = studies.saveAndFlush(Study.create("다른 스터디", "설명", false));
        saveNotice(anotherStudy, START, List.of());

        var result = notices.findCalendarNotices(study.getId(), member.getId(), START, END);

        assertThat(result).extracting(NoticeCalendarProjection::id, NoticeCalendarProjection::createdAt)
                .containsExactlyInAnyOrder(tuple(first.getId(), START), tuple(last.getId(), END.minusNanos(1000)));
    }

    @Test
    @DisplayName("가입 전 미대상 공지도 포함하고 본인의 읽음·미읽음·미대상을 중복 없이 구분한다")
    void includesUnassignedNoticesAndUsesOnlyRequestersReadStatus() {
        Notice unassigned = saveNotice(study, START, List.of(other));
        unassigned.getRecipients().getFirst().markAsRead(START.plusHours(1));
        Notice unread = saveNotice(study, START.plusDays(1), List.of(member, other));
        unread.getRecipients().getLast().markAsRead(START.plusDays(2));
        Notice read = saveNotice(study, START.plusDays(2), List.of(member, other));
        read.getRecipients().getFirst().markAsRead(START.plusDays(3));
        notices.flush();
        jdbc.update("UPDATE study_members SET created_at = ? WHERE id = ?", START.plusDays(5), member.getId());

        var result = notices.findCalendarNotices(study.getId(), member.getId(), START, END);

        assertThat(result).extracting(NoticeCalendarProjection::id, NoticeCalendarProjection::readStatus)
                .containsExactlyInAnyOrder(tuple(unassigned.getId(), NoticeReadStatus.NOT_ASSIGNED),
                        tuple(unread.getId(), NoticeReadStatus.UNREAD), tuple(read.getId(), NoticeReadStatus.READ));
        assertThat(result).allSatisfy(item -> {
            assertThat(item.title()).isEqualTo("월간 공지");
            assertThat(item.createdAt()).isBefore(START.plusDays(5));
        });
    }

    @Test
    @DisplayName("기존 기본 페이지 크기를 넘는 공지도 모두 반환하고 빈 월은 빈 목록을 반환한다")
    void returnsAllMonthlyNoticesWithoutPagination() {
        for (int index = 0; index < 12; index++) {
            saveNotice(study, START.plusDays(index), List.of());
        }

        assertThat(notices.findCalendarNotices(study.getId(), member.getId(), START, END))
                .hasSize(12)
                .allSatisfy(item -> assertThat(item.readStatus()).isEqualTo(NoticeReadStatus.NOT_ASSIGNED));
        assertThat(notices.findCalendarNotices(study.getId(), member.getId(), END, END.plusMonths(1))).isEmpty();
    }

    private StudyMember saveMember(String name) {
        User user = users.saveAndFlush(User.create(name, null));
        return members.saveAndFlush(StudyMember.create(study, user, name, null, StudyMemberRole.MEMBER));
    }

    private Notice saveNotice(Study owner, LocalDateTime createdAt, List<StudyMember> recipients) {
        Notice notice = Notice.create(owner, "월간 공지", "본문");
        notice.addRecipients(recipients);
        notices.saveAndFlush(notice);
        jdbc.update("UPDATE notices SET created_at = ? WHERE id = ?", createdAt, notice.getId());
        return notice;
    }
}
