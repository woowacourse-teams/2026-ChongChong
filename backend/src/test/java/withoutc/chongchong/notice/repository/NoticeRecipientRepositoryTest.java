package withoutc.chongchong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.repository.projection.NoticeReadStatusProjection;
import withoutc.chongchong.notice.repository.projection.NoticeRecipientStatusProjection;
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
class NoticeRecipientRepositoryTest {

    @Autowired
    private NoticeRecipientRepository noticeRecipientRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("여러 공지의 읽음 상태를 StudyMember id로 한 번에 조회한다")
    void findReadStatusesByNoticeIdsAndMemberIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember writer = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember recipient = createMemberWithIdDifferentFromUserId(study);
        StudyMember otherMember = createMember(study, "다른 스터디원", StudyMemberRole.MEMBER);
        Notice readNotice = Notice.create(study, writer, "읽은 공지", "공지 내용");
        readNotice.addRecipients(List.of(recipient, otherMember));
        NoticeRecipient readRecipient = readNotice.getRecipients().stream()
                .filter(candidate -> candidate.getMember().getId().equals(recipient.getId()))
                .findFirst()
                .orElseThrow();
        ReflectionTestUtils.setField(readRecipient, "readAt", LocalDateTime.of(2026, 8, 20, 10, 0));
        Notice unreadNotice = Notice.create(study, writer, "안 읽은 공지", "공지 내용");
        unreadNotice.addRecipients(List.of(recipient));
        Notice otherNotice = Notice.create(study, writer, "다른 공지", "다른 공지 내용");
        otherNotice.addRecipients(List.of(otherMember));
        noticeRepository.saveAllAndFlush(List.of(readNotice, unreadNotice, otherNotice));

        assertThat(recipient.getId()).isNotEqualTo(recipient.getUser().getId());
        assertThat(noticeRecipientRepository.findReadStatusesByNoticeIdsAndMemberId(
                List.of(readNotice.getId(), unreadNotice.getId(), otherNotice.getId()),
                recipient.getId()
        ))
                .extracting(NoticeReadStatusProjection::noticeId, NoticeReadStatusProjection::isRead)
                .containsExactlyInAnyOrder(
                        tuple(readNotice.getId(), true),
                        tuple(unreadNotice.getId(), false)
                );
    }

    @Test
    @DisplayName("공지 수신자 상태를 읽음 여부와 마지막 공지 리마인드 시각으로 조회한다")
    void findStatusesByNoticeIdTest() {
        LocalDateTime readAt = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime earlierRemindAt = LocalDateTime.of(2026, 8, 20, 11, 0);
        LocalDateTime latestRemindAt = LocalDateTime.of(2026, 8, 20, 12, 0);
        LocalDateTime irrelevantNotificationAt = LocalDateTime.of(2026, 8, 20, 13, 0);
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember writer = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember readMember = createMember(study, "읽은 스터디원", StudyMemberRole.MEMBER);
        StudyMember unreadMember = createMember(study, "리마인드 받은 스터디원", StudyMemberRole.MEMBER);
        StudyMember unreadMemberWithoutReminder = createMember(study, "리마인드 없는 스터디원", StudyMemberRole.MEMBER);
        Notice notice = Notice.create(study, writer, "공지", "공지 내용");
        notice.addRecipients(List.of(readMember, unreadMember, unreadMemberWithoutReminder));
        NoticeRecipient readRecipient = notice.getRecipients().stream()
                .filter(candidate -> candidate.getMember().getId().equals(readMember.getId()))
                .findFirst()
                .orElseThrow();
        ReflectionTestUtils.setField(readRecipient, "readAt", readAt);
        noticeRepository.saveAndFlush(notice);

        insertNotification(study.getId(), unreadMember.getId(), notice.getId(), "NOTICE", earlierRemindAt);
        insertNotification(study.getId(), unreadMember.getId(), notice.getId(), "NOTICE", latestRemindAt);
        insertNotification(study.getId(), unreadMember.getId(), notice.getId(), "ASSIGNMENT", irrelevantNotificationAt);
        insertNotification(study.getId(), unreadMember.getId(), notice.getId() + 1, "NOTICE", irrelevantNotificationAt);

        Map<Long, NoticeRecipientStatusProjection> statusesByMemberId = noticeRecipientRepository
                .findStatusesByNoticeId(notice.getId())
                .stream()
                .collect(Collectors.toMap(NoticeRecipientStatusProjection::memberId, status -> status));

        assertThat(statusesByMemberId)
                .hasSize(3)
                .containsKeys(readMember.getId(), unreadMember.getId(), unreadMemberWithoutReminder.getId());
        assertThat(statusesByMemberId.get(readMember.getId()).isRead()).isTrue();
        assertThat(statusesByMemberId.get(readMember.getId()).lastRemindAt()).isNull();
        assertThat(statusesByMemberId.get(unreadMember.getId()).isRead()).isFalse();
        assertThat(statusesByMemberId.get(unreadMember.getId()).lastRemindAt()).isEqualTo(latestRemindAt);
        assertThat(statusesByMemberId.get(unreadMemberWithoutReminder.getId()).isRead()).isFalse();
        assertThat(statusesByMemberId.get(unreadMemberWithoutReminder.getId()).lastRemindAt()).isNull();
    }

    @Test
    @DisplayName("같은 공지와 스터디원으로 수신자를 중복 저장할 수 없다")
    void rejectDuplicateNoticeRecipientTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember writer = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember recipient = createMember(study, "스터디원", StudyMemberRole.MEMBER);
        Notice notice = noticeRepository.saveAndFlush(
                Notice.create(study, writer, "공지", "공지 내용")
        );
        noticeRecipientRepository.saveAndFlush(NoticeRecipient.create(recipient, notice));

        assertThatThrownBy(() -> noticeRecipientRepository.saveAndFlush(
                NoticeRecipient.create(recipient, notice)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private StudyMember createMemberWithIdDifferentFromUserId(Study study) {
        StudyMember candidate = createMember(study, "스터디원", StudyMemberRole.MEMBER);
        if (!candidate.getId().equals(candidate.getUser().getId())) {
            return candidate;
        }

        userRepository.save(User.create("ID 간격 생성용 사용자", null));
        return createMember(study, "다른 스터디원", StudyMemberRole.MEMBER);
    }

    private StudyMember createMember(Study study, String name, StudyMemberRole role) {
        User user = userRepository.save(User.create(name, null));
        return studyMemberRepository.save(StudyMember.create(study, user, name, null, role));
    }

    private void insertNotification(Long studyId, Long recipientId, Long resourceId, String resourceType,
                                    LocalDateTime createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO notification (
                            study_id, recipient_id, type, resource_id, resource_type, is_read, created_at, updated_at
                        ) VALUES (?, ?, 'REMIND', ?, ?, false, ?, ?)
                        """,
                studyId, recipientId, resourceId, resourceType, createdAt, createdAt);
    }
}
