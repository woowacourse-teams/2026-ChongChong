package withoutc.chongchong.notice.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class NoticeUpdateTransactionTest extends PostgresContainerTest {

    private static final LocalDateTime REMIND_AT = LocalDateTime.of(2099, 1, 1, 0, 0);
    private static final LocalDateTime READ_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

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
    private NoticeRepository noticeRepository;

    @BeforeEach
    void cleanBeforeTest() {
        databaseCleaner.clean();
    }

    @AfterEach
    void cleanAfterTest() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("완료 파일을 공지에 연결하고 제거 파일의 기존 삭제 대기 처리를 함께 저장한다")
    void replaceAttachmentsWithFileStatusChanges() {
        Fixture fixture = createFixture();
        Long retainedId = fixture.existingIds().getFirst();
        Long removedId = fixture.existingIds().getLast();
        Long addedId = fixture.newIds().getFirst();
        List<Long> finalIds = List.of(addedId, retainedId);

        noticeService.update(fixture.userId(), fixture.studyId(), fixture.noticeId(),
                new NoticeUpdateRequest("수정 제목", "수정 내용", List.of(REMIND_AT.plusDays(1)), finalIds));

        assertThat(attachmentFileIds(fixture.noticeId())).containsExactlyElementsOf(finalIds);
        assertThat(jdbcTemplate.queryForList("""
                SELECT sort_order FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order
                """, Integer.class, fixture.noticeId())).containsExactly(0, 1);
        Notice found = noticeRepository.findById(fixture.noticeId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("수정 제목");
        assertThat(found.getContent()).isEqualTo("수정 내용");
        assertThat(remindAt(fixture.noticeId())).isEqualTo(REMIND_AT.plusDays(1));
        assertStatus(retainedId, FileUploadStatus.COMPLETED);
        assertStatus(addedId, FileUploadStatus.COMPLETED);
        assertStatus(removedId, FileUploadStatus.DELETE_PENDING);
        assertStatus(fixture.newIds().getLast(), FileUploadStatus.COMPLETED);
        assertRecipientUnchanged(fixture.noticeId());
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("현재 리더는 이전 리더의 첨부를 업로드 재검증 없이 재정렬할 수 있다")
    void reorderPreviousLeadersAttachments() {
        Fixture fixture = createFixture();
        jdbcTemplate.update("UPDATE study_members SET role = 'MEMBER' WHERE study_id = ?", fixture.studyId());
        User newLeader = userRepository.save(User.create("새 리더", null));
        Study study = studyRepository.findById(fixture.studyId()).orElseThrow();
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, newLeader, "새 리더", null, StudyMemberRole.LEADER));

        noticeService.update(newLeader.getId(), fixture.studyId(), fixture.noticeId(),
                new NoticeUpdateRequest(null, null, null, fixture.existingIds().reversed()));

        assertThat(attachmentFileIds(fixture.noticeId())).containsExactlyElementsOf(fixture.existingIds().reversed());
        assertStatus(fixture.existingIds().getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(fixture.existingIds().getLast(), FileUploadStatus.COMPLETED);
        assertRecipientUnchanged(fixture.noticeId());
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("첨부 필드가 null이면 기존 첨부 행과 파일 상태를 유지하면서 제목만 수정한다")
    void preserveAttachmentsWhenNotRequested() {
        Fixture fixture = createFixture();
        List<Long> originalAttachmentIds = attachmentIds(fixture.noticeId());

        noticeService.update(fixture.userId(), fixture.studyId(), fixture.noticeId(),
                new NoticeUpdateRequest("제목만 수정", null, null, null));

        assertThat(attachmentIds(fixture.noticeId())).containsExactlyElementsOf(originalAttachmentIds);
        assertThat(attachmentFileIds(fixture.noticeId())).containsExactlyElementsOf(fixture.existingIds());
        assertThat(noticeRepository.findById(fixture.noticeId()).orElseThrow().getTitle()).isEqualTo("제목만 수정");
        assertStatus(fixture.existingIds().getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(fixture.existingIds().getLast(), FileUploadStatus.COMPLETED);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("빈 최종 목록은 실제 업로드 검증 없이 첨부를 모두 제거하고 기존 파일을 삭제 대기로 바꾼다")
    void removeAllAttachments() {
        Fixture fixture = createFixture();

        noticeService.update(fixture.userId(), fixture.studyId(), fixture.noticeId(),
                new NoticeUpdateRequest(null, null, null, List.of()));

        assertThat(attachmentFileIds(fixture.noticeId())).isEmpty();
        assertStatus(fixture.existingIds().getFirst(), FileUploadStatus.DELETE_PENDING);
        assertStatus(fixture.existingIds().getLast(), FileUploadStatus.DELETE_PENDING);
        assertStatus(fixture.newIds().getFirst(), FileUploadStatus.COMPLETED);
        assertThat(remindAt(fixture.noticeId())).isEqualTo(REMIND_AT);
        assertRecipientUnchanged(fixture.noticeId());
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("일부 새 파일 검증에 실패하면 제목·내용·리마인더·기존 첨부와 모든 파일 상태를 롤백한다")
    void rollbackWhenNewImagePolicyFails() {
        Fixture fixture = createFixture();
        List<Long> originalAttachmentIds = attachmentIds(fixture.noticeId());
        jdbcTemplate.update("UPDATE file_metadata SET content_type = 'application/pdf' WHERE id = ?",
                fixture.newIds().getLast());

        assertThatThrownBy(() -> noticeService.update(fixture.userId(), fixture.studyId(), fixture.noticeId(),
                new NoticeUpdateRequest("수정 제목", "수정 내용", List.of(REMIND_AT.plusDays(1)), fixture.newIds())))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_NOTICE_IMAGE_TYPE);

        assertOriginalState(fixture, originalAttachmentIds);
        verifyNoInteractions(s3FileMetadataReader);
    }

    @Test
    @DisplayName("첨부 교체 SQL과 파일 상태 변경이 flush된 뒤 실패해도 모든 변경을 롤백한다")
    void rollbackAfterReplacementHasBeenFlushed() {
        Fixture fixture = createFixture();
        List<Long> originalAttachmentIds = attachmentIds(fixture.noticeId());
        doAnswer(invocation -> {
            entityManager.flush();
            assertThat(attachmentFileIds(fixture.noticeId())).containsExactlyElementsOf(fixture.newIds());
            assertStatus(fixture.existingIds().getFirst(), FileUploadStatus.DELETE_PENDING);
            assertStatus(fixture.newIds().getFirst(), FileUploadStatus.COMPLETED);
            assertThat(remindAt(fixture.noticeId())).isEqualTo(REMIND_AT.plusDays(1));
            throw new IllegalStateException("첨부 교체 flush 이후 실패");
        }).when(noticeRepository).save(any(Notice.class));

        assertThatThrownBy(() -> noticeService.update(fixture.userId(), fixture.studyId(), fixture.noticeId(),
                new NoticeUpdateRequest("수정 제목", "수정 내용", List.of(REMIND_AT.plusDays(1)), fixture.newIds())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("첨부 교체 flush 이후 실패");

        assertOriginalState(fixture, originalAttachmentIds);
    }

    @Test
    @DisplayName("같은 공지를 서로 다른 완료 파일로 동시에 교체하면 순서대로 성공하고 최종 첨부가 남는다")
    void serializeConcurrentUpdatesToSameNotice() throws Exception {
        Fixture fixture = createFixture();
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<UpdateResult> first = executor.submit(() -> updateConcurrently(start, fixture, fixture.noticeId(),
                    "첫 번째 수정", List.of(fixture.newIds().getFirst())));
            Future<UpdateResult> second = executor.submit(() -> updateConcurrently(start, fixture, fixture.noticeId(),
                    "두 번째 수정", List.of(fixture.newIds().getLast())));

            assertThat(List.of(first.get(10, SECONDS), second.get(10, SECONDS)))
                    .allMatch(UpdateResult::success);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }

        String title = noticeRepository.findById(fixture.noticeId()).orElseThrow().getTitle();
        Map<String, Long> fileIdByTitle = Map.of(
                "첫 번째 수정", fixture.newIds().getFirst(), "두 번째 수정", fixture.newIds().getLast());
        assertThat(attachmentFileIds(fixture.noticeId())).containsExactly(fileIdByTitle.get(title));
        assertStatus(fixture.existingIds().getFirst(), FileUploadStatus.DELETE_PENDING);
        assertStatus(fixture.existingIds().getLast(), FileUploadStatus.DELETE_PENDING);
        assertThat(fileRepository.findAllById(fixture.newIds())).extracting(FileMetadata::getStatus)
                .containsExactlyInAnyOrder(FileUploadStatus.COMPLETED, FileUploadStatus.DELETE_PENDING);
    }

    @Test
    @DisplayName("서로 다른 공지에 같은 새 파일을 동시에 추가하면 한 요청만 성공하고 실패한 공지는 유지된다")
    void rejectConcurrentNewFileReuseAcrossNotices() throws Exception {
        Fixture fixture = createFixture();
        Study study = studyRepository.findById(fixture.studyId()).orElseThrow();
        Long otherNoticeId = noticeRepository.saveAndFlush(Notice.create(study, "다른 공지", "내용")).getId();
        List<Long> sharedFileIds = List.of(fixture.newIds().getFirst());
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<UpdateResult> results;

        try {
            Future<UpdateResult> first = executor.submit(() -> updateConcurrently(start, fixture, fixture.noticeId(),
                    "첫 번째 수정", sharedFileIds));
            Future<UpdateResult> second = executor.submit(() -> updateConcurrently(start, fixture, otherNoticeId,
                    "두 번째 수정", sharedFileIds));
            results = List.of(first.get(10, SECONDS), second.get(10, SECONDS));
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }

        assertThat(results).filteredOn(UpdateResult::success).hasSize(1);
        assertThat(results).extracting(UpdateResult::errorCode)
                .containsExactlyInAnyOrder(null, FileErrorCode.FILE_NOT_ATTACHABLE);
        assertConcurrentReuseResult(fixture, otherNoticeId, results.getFirst().success());
    }

    private UpdateResult updateConcurrently(CyclicBarrier start, Fixture fixture, Long noticeId,
                                           String title, List<Long> fileIds) throws Exception {
        start.await(10, SECONDS);
        try {
            noticeService.update(fixture.userId(), fixture.studyId(), noticeId,
                    new NoticeUpdateRequest(title, null, null, fileIds));
            return new UpdateResult(true, null);
        } catch (FileException exception) {
            return new UpdateResult(false, (FileErrorCode) exception.getErrorCode());
        }
    }

    private void assertConcurrentReuseResult(Fixture fixture, Long otherNoticeId, boolean firstSucceeded) {
        Long sharedFileId = fixture.newIds().getFirst();
        assertStatus(sharedFileId, FileUploadStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notice_attachments WHERE file_id = ?",
                Long.class, sharedFileId)).isOne();
        if (firstSucceeded) {
            assertThat(attachmentFileIds(fixture.noticeId())).containsExactly(sharedFileId);
            assertThat(attachmentFileIds(otherNoticeId)).isEmpty();
            assertStatus(fixture.existingIds().getFirst(), FileUploadStatus.DELETE_PENDING);
            assertStatus(fixture.existingIds().getLast(), FileUploadStatus.DELETE_PENDING);
            return;
        }
        assertThat(attachmentFileIds(fixture.noticeId())).containsExactlyElementsOf(fixture.existingIds());
        assertThat(noticeRepository.findById(fixture.noticeId()).orElseThrow().getTitle()).isEqualTo("기존 제목");
        assertThat(attachmentFileIds(otherNoticeId)).containsExactly(sharedFileId);
        assertStatus(fixture.existingIds().getFirst(), FileUploadStatus.COMPLETED);
        assertStatus(fixture.existingIds().getLast(), FileUploadStatus.COMPLETED);
    }

    private Fixture createFixture() {
        User leader = userRepository.save(User.create("리더", null));
        User memberUser = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("공지 수정 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        StudyMember member = studyMemberRepository.save(
                StudyMember.create(study, memberUser, "스터디원", null, StudyMemberRole.MEMBER));
        FileMetadata first = saveCompletedFile(leader);
        FileMetadata second = saveCompletedFile(leader);
        FileMetadata third = saveCompletedFile(leader);
        FileMetadata fourth = saveCompletedFile(leader);
        Notice notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addAttachments(List.of(first, second));
        notice.addReminders(List.of(REMIND_AT), READ_AT);
        notice.addRecipients(List.of(member));
        notice.getRecipients().getFirst().markAsRead(READ_AT);
        noticeRepository.saveAndFlush(notice);
        return new Fixture(leader.getId(), study.getId(), notice.getId(), List.of(first.getId(), second.getId()),
                List.of(third.getId(), fourth.getId()));
    }

    private FileMetadata saveCompletedFile(User owner) {
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private List<Long> attachmentFileIds(Long noticeId) {
        return jdbcTemplate.queryForList(
                "SELECT file_id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order", Long.class, noticeId);
    }

    private List<Long> attachmentIds(Long noticeId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order", Long.class, noticeId);
    }

    private LocalDateTime remindAt(Long noticeId) {
        return jdbcTemplate.queryForObject("""
                SELECT remind_at FROM notice_reminders WHERE notice_id = ? AND status = 'PENDING'
                """, LocalDateTime.class, noticeId);
    }

    private void assertStatus(Long fileId, FileUploadStatus expected) {
        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(expected);
    }

    private void assertRecipientUnchanged(Long noticeId) {
        assertThat(jdbcTemplate.queryForList("SELECT read_at FROM notice_recipients WHERE notice_id = ?",
                LocalDateTime.class, noticeId)).containsExactly(READ_AT);
    }

    private void assertOriginalState(Fixture fixture, List<Long> originalAttachmentIds) {
        Notice found = noticeRepository.findById(fixture.noticeId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("기존 제목");
        assertThat(found.getContent()).isEqualTo("기존 내용");
        assertThat(remindAt(fixture.noticeId())).isEqualTo(REMIND_AT);
        assertThat(attachmentIds(fixture.noticeId())).containsExactlyElementsOf(originalAttachmentIds);
        assertThat(attachmentFileIds(fixture.noticeId())).containsExactlyElementsOf(fixture.existingIds());
        assertThat(fileRepository.findAllById(fixture.existingIds())).extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.COMPLETED);
        assertThat(fileRepository.findAllById(fixture.newIds())).extracting(FileMetadata::getStatus)
                .containsOnly(FileUploadStatus.COMPLETED);
        assertRecipientUnchanged(fixture.noticeId());
    }

    private record Fixture(Long userId, Long studyId, Long noticeId, List<Long> existingIds, List<Long> newIds) {
    }

    private record UpdateResult(boolean success, FileErrorCode errorCode) {
    }
}
