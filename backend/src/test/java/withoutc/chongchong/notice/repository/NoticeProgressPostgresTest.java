package withoutc.chongchong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.projection.NoticeProgressProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class NoticeProgressPostgresTest extends PostgresContainerTest {

    @Autowired
    private NoticeRepository noticeRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository memberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("집계 쿼리는 읽음 수신자와 발송된 리마인더를 엔티티 계산과 같게 반영한다")
    void progressMatchesEntityCalculation() {
        LocalDateTime now = LocalDateTime.now();
        Long noticeId = new TransactionTemplate(transactionManager).execute(status -> {
            Study study = studyRepository.save(Study.create("집계 검증", "설명", false));
            List<StudyMember> members = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                User user = userRepository.save(User.create("멤버" + i, null));
                members.add(memberRepository.save(
                        StudyMember.create(study, user, "멤버" + i, null, StudyMemberRole.MEMBER)));
            }
            Notice notice = Notice.create(study, "공지", "내용");
            notice.addRecipients(members);
            notice.addReminders(List.of(now.plusDays(1), now.plusDays(2), now.plusDays(3)), now);
            notice.getReminders().getFirst().markAsSent();
            notice.getRecipients().get(0).markAsRead(now);
            notice.getRecipients().get(1).markAsRead(now);
            return noticeRepository.save(notice).getId();
        });

        NoticeProgressProjection progress = noticeRepository.findProgressByIdIn(List.of(noticeId)).getFirst();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Notice notice = noticeRepository.findById(noticeId).orElseThrow();
            assertThat(progress.recipientCount()).isEqualTo(notice.getRecipientCount()).isEqualTo(4);
            assertThat(progress.readRecipientCount()).isEqualTo(notice.getReadRecipientCount()).isEqualTo(2);
            assertThat(progress.nextRemindAt()).isNotNull().isEqualTo(notice.getNextRemindAt());
        });
    }
}
