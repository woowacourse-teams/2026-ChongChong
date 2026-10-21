package withoutc.chongchong.notice.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class NoticeDeleteTransactionTest extends PostgresContainerTest {

    private static final LocalDateTime READ_AT = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime REMIND_AT = LocalDateTime.of(2099, 1, 1, 0, 0);

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private S3FileMetadataReader s3FileMetadataReader;

    @MockitoSpyBean
    private NotificationService notificationService;

    @MockitoSpyBean
    private NoticeAttachmentService noticeAttachmentService;

    @BeforeEach
    void cleanBeforeTest() {
        databaseCleaner.clean();
    }

    @AfterEach
    void cleanAfterTest() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("이전 리더의 첨부도 삭제 대기로 전환하고 공지 관련 데이터를 삭제하며 다른 공지는 유지한다")
    void deleteNoticeWithAttachments() {
        Fixture fixture = createFixture();
        Map<Long, String> originalKeys = fileRepository.findAll().stream()
                .collect(Collectors.toMap(FileMetadata::getId, FileMetadata::getObjectKey));

        noticeService.delete(fixture.userId(), fixture.studyId(), fixture.noticeId());

        assertDeleted(fixture, FileUploadStatus.COMPLETED);
        assertThat(fileRepository.findAll()).hasSize(4).allSatisfy(file ->
                assertThat(file.getObjectKey()).isEqualTo(originalKeys.get(file.getId())));
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("첨부 없는 기존 공지를 삭제해도 다른 공지와 파일 상태는 변경하지 않는다")
    void deleteNoticeWithoutAttachments() {
        Fixture fixture = createFixture();
        Study study = studyRepository.findById(fixture.studyId()).orElseThrow();
        Long emptyNoticeId = noticeRepository.saveAndFlush(Notice.create(study, "무첨부 공지", "내용")).getId();

        noticeService.delete(fixture.userId(), fixture.studyId(), emptyNoticeId);

        assertThat(noticeRepository.existsById(emptyNoticeId)).isFalse();
        assertOriginalState(fixture);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("알림 삭제 SQL 실행 후 실패하면 파일 상태·공지·첨부·리마인더·수신자·알림을 롤백한다")
    void rollbackWhenNotificationDeletionFails() {
        Fixture fixture = createFixture();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            entityManager.flush();
            assertThat(notificationCount(fixture.noticeId())).isZero();
            assertStatus(fixture.fileIds().getFirst(), FileUploadStatus.DELETE_PENDING);
            throw new IllegalStateException("알림 삭제 후 실패");
        }).when(notificationService).deleteNotificationsForResource(ResourceType.NOTICE, fixture.noticeId());

        assertThatThrownBy(() -> noticeService.delete(fixture.userId(), fixture.studyId(), fixture.noticeId()))
                .isInstanceOf(IllegalStateException.class).hasMessage("알림 삭제 후 실패");

        assertOriginalState(fixture);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("공지 삭제 SQL까지 flush한 뒤 같은 트랜잭션에서 실패하면 모든 데이터를 복구한다")
    void rollbackAfterDeletionHasBeenFlushed() {
        Fixture fixture = createFixture();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            noticeService.delete(fixture.userId(), fixture.studyId(), fixture.noticeId());
            entityManager.flush();
            assertDeleted(fixture, FileUploadStatus.COMPLETED);
            throw new IllegalStateException("공지 삭제 flush 후 실패");
        })).isInstanceOf(IllegalStateException.class).hasMessage("공지 삭제 flush 후 실패");

        assertOriginalState(fixture);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("일반 스터디원은 첨부 공지를 삭제하지 못하며 기존 데이터도 유지된다")
    void rejectMemberDeletion() {
        Fixture fixture = createFixture();

        assertThatThrownBy(() -> noticeService.delete(fixture.memberId(), fixture.studyId(), fixture.noticeId()))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ACCESS_DENIED);

        assertOriginalState(fixture);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("수정이 먼저 잠금을 획득하면 삭제는 최신 첨부를 조회하여 새 파일까지 삭제 대기로 전환한다")
    void deleteAfterConcurrentUpdate() throws Exception {
        Fixture fixture = createFixture();
        CountDownLatch verifying = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);
        CountDownLatch deletionStarted = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object preparedFiles = invocation.callRealMethod();
            verifying.countDown();
            awaitLatch(releaseUpdate);
            return preparedFiles;
        }).when(noticeAttachmentService).prepareForUpdate(anyLong(), any(Notice.class), anyList());
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> update = executor.submit(() -> noticeService.update(
                    fixture.userId(), fixture.studyId(), fixture.noticeId(),
                    new NoticeUpdateRequest("수정 제목", null, null, List.of(fixture.newFileId()))));
            awaitLatch(verifying);
            Future<?> deletion = executor.submit(() -> {
                deletionStarted.countDown();
                noticeService.delete(fixture.userId(), fixture.studyId(), fixture.noticeId());
            });
            awaitLatch(deletionStarted);
            releaseUpdate.countDown();
            update.get(10, SECONDS);
            deletion.get(10, SECONDS);
        } finally {
            releaseUpdate.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }

        assertDeleted(fixture, FileUploadStatus.DELETE_PENDING);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("삭제가 먼저 잠금을 획득하면 대기한 수정은 404로 실패하고 새 파일은 선행 완료 상태를 유지한다")
    void rejectUpdateAfterConcurrentDeletion() throws Exception {
        Fixture fixture = createFixture();
        CountDownLatch deleting = new CountDownLatch(1);
        CountDownLatch releaseDeletion = new CountDownLatch(1);
        CountDownLatch updateStarted = new CountDownLatch(1);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            deleting.countDown();
            awaitLatch(releaseDeletion);
            return null;
        }).when(notificationService).deleteNotificationsForResource(ResourceType.NOTICE, fixture.noticeId());
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> deletion = executor.submit(() ->
                    noticeService.delete(fixture.userId(), fixture.studyId(), fixture.noticeId()));
            awaitLatch(deleting);
            Future<NoticeErrorCode> update = executor.submit(() -> {
                updateStarted.countDown();
                return updateAfterDeletion(fixture);
            });
            awaitLatch(updateStarted);
            releaseDeletion.countDown();
            deletion.get(10, SECONDS);
            assertThat(update.get(10, SECONDS)).isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
        } finally {
            releaseDeletion.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }

        assertDeleted(fixture, FileUploadStatus.COMPLETED);
        verifyNoInteractions(s3FileMetadataReader);
    }

    private NoticeErrorCode updateAfterDeletion(Fixture fixture) {
        try {
            noticeService.update(fixture.userId(), fixture.studyId(), fixture.noticeId(),
                    new NoticeUpdateRequest("수정 제목", null, null, List.of(fixture.newFileId())));
            return null;
        } catch (NoticeException exception) {
            return (NoticeErrorCode) exception.getErrorCode();
        }
    }

    private void awaitLatch(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(10, SECONDS)).isTrue();
    }

    private Fixture createFixture() {
        User leader = userRepository.save(User.create("현재 리더", null));
        User previousLeader = userRepository.save(User.create("이전 리더", null));
        User memberUser = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("공지 삭제 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        StudyMember member = studyMemberRepository.save(
                StudyMember.create(study, memberUser, "스터디원", null, StudyMemberRole.MEMBER));
        FileMetadata first = saveFile(previousLeader, true);
        FileMetadata second = saveFile(previousLeader, true);
        FileMetadata added = saveFile(leader, true);
        FileMetadata otherFile = saveFile(previousLeader, true);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addAttachments(List.of(second, first));
        notice.addRecipients(List.of(member));
        notice.getRecipients().getFirst().markAsRead(READ_AT);
        notice.addReminders(List.of(REMIND_AT), READ_AT);
        noticeRepository.saveAndFlush(notice);
        Notice otherNotice = Notice.create(study, "다른 공지", "다른 내용");
        otherNotice.addAttachments(List.of(otherFile));
        noticeRepository.saveAndFlush(otherNotice);
        notificationRepository.saveAllAndFlush(List.of(
                notification(memberUser, notice.getId(), NotificationType.NEW),
                notification(memberUser, notice.getId(), NotificationType.REMIND),
                notification(memberUser, otherNotice.getId(), NotificationType.NEW)));
        return new Fixture(leader.getId(), memberUser.getId(), study.getId(), notice.getId(),
                List.of(second.getId(), first.getId()), added.getId(), otherNotice.getId(), otherFile.getId());
    }

    private FileMetadata saveFile(User owner, boolean completed) {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        if (completed) {
            file.completeUpload();
        }
        return fileRepository.saveAndFlush(file);
    }

    private Notification notification(User recipient, Long noticeId, NotificationType type) {
        return Notification.create(recipient, "공지 알림", "내용", type, noticeId, ResourceType.NOTICE,
                "/notices/" + noticeId);
    }

    private long countNoticeRows(String tableName, Long noticeId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE notice_id = ?",
                Long.class, noticeId);
    }

    private long notificationCount(Long noticeId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE resource_type = 'NOTICE' AND resource_id = ?",
                Long.class, noticeId);
    }

    private void assertStatus(Long fileId, FileUploadStatus status) {
        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(status);
    }

    private void assertOtherNoticeUnchanged(Fixture fixture) {
        assertThat(noticeRepository.findById(fixture.otherNoticeId()).orElseThrow().getTitle()).isEqualTo("다른 공지");
        assertThat(jdbcTemplate.queryForList("SELECT file_id FROM notice_attachments WHERE notice_id = ?",
                Long.class, fixture.otherNoticeId())).containsExactly(fixture.otherFileId());
        assertStatus(fixture.otherFileId(), FileUploadStatus.COMPLETED);
        assertThat(notificationCount(fixture.otherNoticeId())).isOne();
    }

    private void assertDeleted(Fixture fixture, FileUploadStatus newFileStatus) {
        assertThat(noticeRepository.existsById(fixture.noticeId())).isFalse();
        assertThat(countNoticeRows("notice_attachments", fixture.noticeId())).isZero();
        assertThat(countNoticeRows("notice_reminders", fixture.noticeId())).isZero();
        assertThat(countNoticeRows("notice_recipients", fixture.noticeId())).isZero();
        assertThat(notificationCount(fixture.noticeId())).isZero();
        assertStatus(fixture.fileIds().getFirst(), FileUploadStatus.DELETE_PENDING);
        assertStatus(fixture.fileIds().getLast(), FileUploadStatus.DELETE_PENDING);
        assertStatus(fixture.newFileId(), newFileStatus);
        assertOtherNoticeUnchanged(fixture);
    }

    private void assertOriginalState(Fixture fixture) {
        assertThat(noticeRepository.findById(fixture.noticeId()).orElseThrow().getTitle()).isEqualTo("기존 제목");
        assertThat(jdbcTemplate.queryForList("SELECT file_id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order",
                Long.class, fixture.noticeId())).containsExactlyElementsOf(fixture.fileIds());
        assertThat(jdbcTemplate.queryForList("SELECT remind_at FROM notice_reminders WHERE notice_id = ?",
                LocalDateTime.class, fixture.noticeId())).containsExactly(REMIND_AT);
        assertThat(jdbcTemplate.queryForList("SELECT read_at FROM notice_recipients WHERE notice_id = ?",
                LocalDateTime.class, fixture.noticeId())).containsExactly(READ_AT);
        assertThat(notificationCount(fixture.noticeId())).isEqualTo(2);
        assertStatus(fixture.fileIds().getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(fixture.fileIds().getLast(), FileUploadStatus.COMPLETED);
        assertStatus(fixture.newFileId(), FileUploadStatus.COMPLETED);
        assertOtherNoticeUnchanged(fixture);
    }

    private record Fixture(Long userId, Long memberId, Long studyId, Long noticeId, List<Long> fileIds,
                           Long newFileId, Long otherNoticeId, Long otherFileId) {
    }
}
