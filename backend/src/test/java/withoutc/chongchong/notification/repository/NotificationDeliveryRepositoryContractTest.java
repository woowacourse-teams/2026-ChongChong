package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

abstract class NotificationDeliveryRepositoryContractTest {

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("정상적인 알림 발송 기록을 저장하고 허용되지 않은 상태는 거부한다")
    void saveValidDeliveryAndRejectInvalidStatus() {
        DeliveryFixture fixture = saveFixture();

        NotificationDelivery saved = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(fixture.notification(), fixture.pushToken())
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
    @DisplayName("같은 알림과 푸시 토큰으로 발송 기록을 중복 저장할 수 없다")
    void rejectDuplicateDelivery() {
        DeliveryFixture fixture = saveFixture();
        notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(fixture.notification(), fixture.pushToken())
        );

        assertThatThrownBy(() -> notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(fixture.notification(), fixture.pushToken())
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private DeliveryFixture saveFixture() {
        String installationId = "notification-delivery-installation-" + UUID.randomUUID();
        User user = userRepository.saveAndFlush(User.create("알림 수신자", null));
        Study study = studyRepository.saveAndFlush(Study.create("알림 테스트 스터디", "설명"));
        StudyMember recipient = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, user.getName(), null, StudyMemberRole.MEMBER)
        );
        PushToken pushToken = pushTokenRepository.saveAndFlush(
                PushToken.create(user, installationId, TokenProvider.EXPO, "push-token", DevicePlatform.ANDROID)
        );
        Notification notification = saveNotification(study, recipient);
        return new DeliveryFixture(notification, pushToken);
    }

    private Notification saveNotification(Study study, StudyMember recipient) {
        Notification notification = BeanUtils.instantiateClass(Notification.class);
        ReflectionTestUtils.setField(notification, "study", study);
        ReflectionTestUtils.setField(notification, "recipient", recipient);
        ReflectionTestUtils.setField(notification, "type", NotificationType.REMIND);
        ReflectionTestUtils.setField(notification, "resourceId", 1L);
        ReflectionTestUtils.setField(notification, "resourceType", NotificationResourceType.NOTICE);
        return notificationRepository.saveAndFlush(notification);
    }

    private record DeliveryFixture(Notification notification, PushToken pushToken) {
    }
}
