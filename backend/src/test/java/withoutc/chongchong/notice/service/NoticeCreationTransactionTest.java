package withoutc.chongchong.notice.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.controller.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.entity.Notice;
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

class NoticeCreationTransactionTest extends PostgresContainerTest {

    private static final LocalDateTime REMIND_AT = LocalDateTime.of(2099, 1, 1, 0, 0);

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private S3FileMetadataReader s3FileMetadataReader;

    @MockitoSpyBean
    private NotificationService notificationService;

    @BeforeEach
    void cleanBeforeTest() {
        databaseCleaner.clean();
    }

    @AfterEach
    void cleanAfterTest() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("완료된 파일을 요청 순서대로 공지에 연결하고 완료 상태를 유지하며 알림을 저장한다")
    void createNoticeWithCompletedAttachments() {
        Fixture fixture = createFixture();
        List<Long> requestedIds = fixture.fileIds().reversed();

        NoticeCreateResponse response = noticeService.create(
                fixture.userId(), fixture.studyId(), createRequest(requestedIds)
        );

        assertThat(response.noticeId()).isNotNull();
        assertThat(jdbcTemplate.queryForList("""
                SELECT file_id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order
                """, Long.class, response.noticeId())).containsExactlyElementsOf(requestedIds);
        assertThat(fileRepository.findAllById(fixture.fileIds()))
                .hasSize(2)
                .extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.COMPLETED);
        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_recipients")).isOne();
        assertThat(countRows("notice_reminders")).isOne();
        assertThat(countRows("notifications")).isOne();
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("두 번째 파일이 미완료이면 공지 저장을 거부하고 각 파일의 원래 상태를 유지한다")
    void rejectWhenOneFileIsPending() {
        Fixture fixture = createFixture();
        jdbcTemplate.update("UPDATE file_metadata SET status = 'PENDING' WHERE id = ?", fixture.fileIds().getLast());

        assertThatThrownBy(() -> noticeService.create(
                fixture.userId(), fixture.studyId(), createRequest(fixture.fileIds())
        ))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_ATTACHABLE);

        assertNoNoticeData();
        assertThat(fileRepository.findById(fixture.fileIds().getFirst()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(fileRepository.findById(fixture.fileIds().getLast()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.PENDING);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("알림 생성 후 실패하면 공지 변경을 롤백하고 선행 완료 파일은 완료 상태로 유지한다")
    void rollbackAfterNotificationCreationFails() {
        Fixture fixture = createFixture();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            entityManager.flush();
            assertThat(countRows("notice_attachments")).isEqualTo(2);
            assertThat(countRows("notifications")).isOne();
            throw new IllegalStateException("알림 생성 후 실패");
        }).when(notificationService).createNoticeCreatedEventNotifications(any(Notice.class), anyList());

        assertThatThrownBy(() -> noticeService.create(
                fixture.userId(), fixture.studyId(), createRequest(fixture.fileIds())
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("알림 생성 후 실패");

        assertNoNoticeData();
        assertFilesCompleted(fixture);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("첨부 목록이 null이거나 비어 있으면 S3 조회 없이 공지를 생성하고 파일 상태를 유지한다")
    void createWithoutAttachments(List<Long> fileIds) {
        Fixture fixture = createFixture();

        NoticeCreateResponse response = noticeService.create(
                fixture.userId(), fixture.studyId(), createRequest(fileIds)
        );

        assertThat(response.noticeId()).isNotNull();
        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notifications")).isOne();
        assertFilesCompleted(fixture);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("같은 파일들을 반대 순서로 동시 첨부해도 한 공지만 저장되고 파일은 중복 연결되지 않는다")
    void rejectConcurrentAttachmentReuse() throws Exception {
        Fixture fixture = createFixture();
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<CreationResult> first = executor.submit(() ->
                    createConcurrently(start, fixture, fixture.fileIds()));
            Future<CreationResult> second = executor.submit(() ->
                    createConcurrently(start, fixture, fixture.fileIds().reversed()));
            List<CreationResult> results = List.of(first.get(10, SECONDS), second.get(10, SECONDS));

            assertThat(results).filteredOn(result -> result.noticeId() != null).hasSize(1);
            assertThat(results).extracting(CreationResult::errorCode)
                    .containsExactlyInAnyOrder(null, FileErrorCode.FILE_NOT_ATTACHABLE);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }

        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_attachments")).isEqualTo(2);
        assertThat(countRows("notifications")).isOne();
        assertThat(fileRepository.findAllById(fixture.fileIds()))
                .extracting(FileMetadata::getStatus).containsOnly(FileUploadStatus.COMPLETED);
    }

    private CreationResult createConcurrently(CyclicBarrier start, Fixture fixture, List<Long> fileIds) throws Exception {
        start.await(10, SECONDS);
        try {
            NoticeCreateResponse response = noticeService.create(
                    fixture.userId(), fixture.studyId(), createRequest(fileIds)
            );
            return new CreationResult(response.noticeId(), null);
        } catch (FileException exception) {
            return new CreationResult(null, (FileErrorCode) exception.getErrorCode());
        }
    }

    private Fixture createFixture() {
        User leader = userRepository.save(User.create("리더", null));
        User member = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("첨부 트랜잭션 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        FileMetadata first = FileMetadata.create(leader, "first.png", "uploads/first.png", "image/png", 1024);
        FileMetadata second = FileMetadata.create(leader, "second.png", "uploads/second.png", "image/png", 2048);
        first.completeUpload();
        second.completeUpload();
        fileRepository.saveAllAndFlush(List.of(first, second));
        return new Fixture(leader.getId(), study.getId(), List.of(first.getId(), second.getId()));
    }

    private NoticeCreateRequest createRequest(List<Long> fileIds) {
        return new NoticeCreateRequest("첨부 공지", "공지 내용", List.of(REMIND_AT), fileIds);
    }

    private void assertNoNoticeData() {
        assertThat(countRows("notices")).isZero();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notice_recipients")).isZero();
        assertThat(countRows("notice_reminders")).isZero();
        assertThat(countRows("notifications")).isZero();
        assertThat(countRows("notification_deliveries")).isZero();
    }

    private void assertFilesCompleted(Fixture fixture) {
        assertThat(fileRepository.findAllById(fixture.fileIds()))
                .hasSize(2)
                .extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.COMPLETED);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }

    private record Fixture(Long userId, Long studyId, List<Long> fileIds) {
    }

    private record CreationResult(Long noticeId, FileErrorCode errorCode) {
    }
}
