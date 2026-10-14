package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

abstract class NotificationDeliveryRepositoryContractTest {

    @Autowired
    protected NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Autowired
    protected StudyRepository studyRepository;

    @Autowired
    protected StudyMemberRepository studyMemberRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("정상적인 알림 발송 기록을 저장하고 허용되지 않은 상태는 거부한다")
    void saveValidDeliveryAndRejectInvalidStatus() {
        DeliveryFixture fixture = saveFixture();

        NotificationDelivery saved = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(fixture.notification(), fixture.webPushSubscription())
        );

        assertThat(notificationDeliveryRepository.count()).isOne();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.PENDING);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE notification_deliveries SET status = ? WHERE id = ?",
                "INVALID",
                saved.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 알림과 Web Push 구독으로 발송 기록을 중복 저장할 수 없다")
    void rejectDuplicateDelivery() {
        DeliveryFixture fixture = saveFixture();
        notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(fixture.notification(), fixture.webPushSubscription())
        );

        assertThatThrownBy(() -> notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(fixture.notification(), fixture.webPushSubscription())
                )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("claim 가능한 대기열 수와 가장 오래 기다린 작업을 집계한다")
    void findClaimableQueueSnapshot() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        var before = notificationDeliveryRepository.findClaimableQueueSnapshot(
                now, DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAIT);

        NotificationDelivery pending = saveDelivery(saveFixture());
        NotificationDelivery dueRetry = saveDelivery(saveFixture());
        NotificationDelivery futureRetry = saveDelivery(saveFixture());
        LocalDateTime dueRetryAt = now.minusMinutes(1);
        jdbcTemplate.update("UPDATE notification_deliveries SET status = 'RETRY_WAIT', next_retry_at = ? WHERE id = ?",
                dueRetryAt, dueRetry.getId());
        jdbcTemplate.update("UPDATE notification_deliveries SET status = 'RETRY_WAIT', next_retry_at = ? WHERE id = ?",
                now.plusMinutes(1), futureRetry.getId());

        var after = notificationDeliveryRepository.findClaimableQueueSnapshot(
                now, DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAIT);

        assertThat(after.readyCount()).isEqualTo(before.readyCount() + 2);
        assertThat(pending.getCreatedAt()).isNotNull();
        LocalDateTime expectedOldestReadyAt = pending.getCreatedAt();
        if (dueRetryAt.isBefore(expectedOldestReadyAt)) {
            expectedOldestReadyAt = dueRetryAt;
        }
        if (before.oldestReadyAt() != null && before.oldestReadyAt().isBefore(expectedOldestReadyAt)) {
            expectedOldestReadyAt = before.oldestReadyAt();
        }
        assertThat(after.oldestReadyAt()).isEqualTo(expectedOldestReadyAt);
    }

    protected NotificationDelivery saveDelivery(DeliveryFixture fixture) {
        return notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(fixture.notification(), fixture.webPushSubscription())
        );
    }

    protected DeliveryFixture saveFixture() {
        String endpoint = "https://push.example.com/notification-delivery-" + UUID.randomUUID();
        User user = userRepository.saveAndFlush(User.create("알림 수신자", null));
        Study study = studyRepository.saveAndFlush(Study.create("알림 테스트 스터디", "설명"));
        StudyMember recipient = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, user.getName(), null, StudyMemberRole.MEMBER)
        );
        WebPushSubscription webPushSubscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(
                        user, UUID.randomUUID().toString(), endpoint,
                        "p256dh-key", "auth-secret"
                )
        );
        Notification notification = saveNotification(study, recipient);
        return new DeliveryFixture(notification, webPushSubscription);
    }

    protected Notification saveNotification(Study study, StudyMember recipient) {
        Notification notification = Notification.create(
                recipient.getUser(),
                "[스터디] 새 공지",
                "공지 제목",
                NotificationType.REMIND,
                1L,
                ResourceType.NOTICE,
                "/studies/%d/notices/1".formatted(study.getId())
        );
        return notificationRepository.saveAndFlush(notification);
    }

    protected record DeliveryFixture(Notification notification, WebPushSubscription webPushSubscription) {
    }
}
