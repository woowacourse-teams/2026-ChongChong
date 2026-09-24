package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
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
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자가 받은 알림만 모두 삭제한다")
    void deleteAllByRecipientIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember target = createMember(study, "삭제 대상");
        StudyMember otherMember = createMember(study, "다른 스터디원");
        saveNotification(study, target, 1L);
        saveNotification(study, otherMember, 1L);

        int deletedCount = notificationRepository.deleteAllByRecipientId(target.getUser().getId());

        assertThat(deletedCount).isOne();
        assertThat(notificationRepository.findAll())
                .extracting(notification -> notification.getRecipient().getId())
                .containsExactly(otherMember.getUser().getId());
    }

    private StudyMember createMember(Study study, String name) {
        User user = userRepository.save(User.create(name, null));
        return studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, name, null, StudyMemberRole.MEMBER)
        );
    }

    private void saveNotification(Study study, StudyMember recipient, Long resourceId) {
        Notification notification = Notification.create(
                recipient.getUser(),
                "[스터디] 새 공지",
                "공지 제목",
                NotificationType.REMIND,
                resourceId,
                ResourceType.NOTICE,
                "/studies/%d/notices/%d".formatted(study.getId(), resourceId)
        );
        notificationRepository.saveAndFlush(notification);
    }
}
