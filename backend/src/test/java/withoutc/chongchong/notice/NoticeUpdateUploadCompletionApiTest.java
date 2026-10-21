package withoutc.chongchong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.notice.entity.Notice;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "file.upload.max-size-bytes=5242880",
        "notice.attachments.max-count=10",
        "notice.attachments.max-size-bytes=5242880",
        "notice.attachments.allowed-content-types=image/jpeg,image/png,image/webp"
})
class NoticeUpdateUploadCompletionApiTest extends PostgresContainerTest {

    private static final LocalDateTime REMIND_AT = LocalDateTime.of(2099, 1, 1, 0, 0);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private S3FileMetadataReader metadataReader;

    @LocalServerPort
    private int port;

    private User leader;
    private Study study;
    private Notice notice;
    private FileMetadata existingFile;
    private List<Long> originalAttachmentIds;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("리더", null));
        study = studyRepository.save(Study.create("공지 수정 완료 연동 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        existingFile = savePendingFile();
        existingFile.completeUpload();
        fileRepository.saveAndFlush(existingFile);
        notice = Notice.create(study, "기존 제목", "기존 내용");
        notice.addAttachments(List.of(existingFile));
        notice.addReminders(List.of(REMIND_AT), LocalDateTime.of(2026, 1, 1, 0, 0));
        noticeRepository.saveAndFlush(notice);
        originalAttachmentIds = attachmentIds();
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("완료 API를 통과한 파일을 기존 첨부와 함께 요청 순서로 수정하며 S3를 다시 조회하지 않는다")
    void updateAfterCompletingNewUpload() {
        FileMetadata added = savePendingFile();
        when(metadataReader.findSize(added.getObjectKey())).thenReturn(OptionalLong.of(added.getSize()));
        complete(added).then().statusCode(204);
        List<Long> finalIds = List.of(added.getId(), existingFile.getId());

        patch(leader, notice.getId(), updateBody(finalIds)).then().statusCode(204);

        assertFileIds(notice.getId(), finalIds);
        Notice found = noticeRepository.findById(notice.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("수정 제목");
        assertThat(found.getContent()).isEqualTo("수정 내용");
        assertCompleted(added);
        assertCompleted(existingFile);
        verify(metadataReader).findSize(added.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @Test
    @DisplayName("일부 업로드 완료 실패 시 수정 전체를 보존하고 나머지 완료 후 같은 파일들로 재시도한다")
    void retryUpdateAfterCompletingRemainingUpload() {
        FileMetadata first = savePendingFile();
        FileMetadata second = savePendingFile();
        when(metadataReader.findSize(first.getObjectKey())).thenReturn(OptionalLong.of(first.getSize()));
        when(metadataReader.findSize(second.getObjectKey()))
                .thenReturn(OptionalLong.empty(), OptionalLong.of(second.getSize()));
        complete(first).then().statusCode(204);
        complete(second).then().statusCode(409).body("code", equalTo("FILE_UPLOAD_NOT_FOUND"));
        List<Long> finalIds = List.of(first.getId(), second.getId());

        patch(leader, notice.getId(), updateBody(finalIds)).then().statusCode(400)
                .body("code", equalTo("FILE_NOT_ATTACHABLE"));

        assertOriginalNotice();
        assertCompleted(first);
        assertThat(fileRepository.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.PENDING);

        complete(second).then().statusCode(204);
        patch(leader, notice.getId(), updateBody(finalIds)).then().statusCode(204);

        assertFileIds(notice.getId(), finalIds);
        assertCompleted(existingFile);
        assertCompleted(first);
        assertCompleted(second);
        verify(metadataReader).findSize(first.getObjectKey());
        verify(metadataReader, times(2)).findSize(second.getObjectKey());
        verifyNoMoreInteractions(metadataReader);
    }

    @Test
    @DisplayName("제거한 완료 파일은 완료 API 재호출 없이 같은 공지에 다시 첨부할 수 있다")
    void reattachRemovedFileToSameNotice() {
        patchAttachments(leader, notice.getId(), List.of()).then().statusCode(204);
        assertFileIds(notice.getId(), List.of());
        assertCompleted(existingFile);

        patchAttachments(leader, notice.getId(), List.of(existingFile.getId())).then().statusCode(204);

        assertFileIds(notice.getId(), List.of(existingFile.getId()));
        assertCompleted(existingFile);
        assertThat(fileRepository.findById(existingFile.getId()).orElseThrow().getObjectKey())
                .isEqualTo(existingFile.getObjectKey());
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("제거 파일을 다른 공지로 옮긴 뒤에는 원래 공지가 같은 파일을 중복 첨부할 수 없다")
    void transferRemovedFileToAnotherNotice() {
        Long otherNoticeId = noticeRepository.saveAndFlush(Notice.create(study, "다른 공지", "다른 내용")).getId();
        patchAttachments(leader, notice.getId(), List.of()).then().statusCode(204);
        patchAttachments(leader, otherNoticeId, List.of(existingFile.getId())).then().statusCode(204);

        patchAttachments(leader, notice.getId(), List.of(existingFile.getId())).then().statusCode(400)
                .body("code", equalTo("FILE_NOT_ATTACHABLE"));

        assertFileIds(notice.getId(), List.of());
        assertFileIds(otherNoticeId, List.of(existingFile.getId()));
        assertCompleted(existingFile);
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("새 리더는 이전 리더 파일을 유지할 수 있지만 제거 후 새 첨부로 다시 추가할 수 없다")
    void recheckOwnershipWhenReattachingPreviousLeadersFile() {
        jdbcTemplate.update("UPDATE study_members SET role = 'MEMBER' WHERE study_id = ?", study.getId());
        User newLeader = userRepository.save(User.create("새 리더", null));
        studyMemberRepository.saveAndFlush(StudyMember.create(study, newLeader, "새 리더", null, StudyMemberRole.LEADER));
        patchAttachments(newLeader, notice.getId(), List.of(existingFile.getId())).then().statusCode(204);
        patchAttachments(newLeader, notice.getId(), List.of()).then().statusCode(204);

        patchAttachments(newLeader, notice.getId(), List.of(existingFile.getId())).then().statusCode(403)
                .body("code", equalTo("FILE_NOT_OWNED"));

        assertFileIds(notice.getId(), List.of());
        assertCompleted(existingFile);
        verifyNoInteractions(metadataReader);
    }

    private FileMetadata savePendingFile() {
        return fileRepository.saveAndFlush(FileMetadata.create(
                leader, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024));
    }

    private Response complete(FileMetadata file) {
        return testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .post("/files/{fileId}/complete", file.getId());
    }

    private Response patchAttachments(User actor, Long noticeId, List<Long> fileIds) {
        return patch(actor, noticeId, Map.of("attachmentFileIds", fileIds));
    }

    private Response patch(User actor, Long noticeId, Map<String, Object> body) {
        return testAuthRequest.givenAuthenticatedUser(actor.getId()).port(port).contentType(ContentType.JSON)
                .body(body).patch("/studies/{studyId}/notices/{noticeId}", study.getId(), noticeId);
    }

    private Map<String, Object> updateBody(List<Long> fileIds) {
        return Map.of("title", "수정 제목", "content", "수정 내용", "remindAts", List.of("2099-01-02T00:00:00"),
                "attachmentFileIds", fileIds);
    }

    private void assertFileIds(Long noticeId, List<Long> fileIds) {
        Response detail = testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{studyId}/notices/{noticeId}", study.getId(), noticeId);
        detail.then().statusCode(200);
        assertThat(detail.jsonPath().getList("attachments.fileId", Long.class)).containsExactlyElementsOf(fileIds);
    }

    private List<Long> attachmentIds() {
        return jdbcTemplate.queryForList("SELECT id FROM notice_attachments WHERE notice_id = ? ORDER BY sort_order",
                Long.class, notice.getId());
    }

    private void assertCompleted(FileMetadata file) {
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    private void assertOriginalNotice() {
        Notice found = noticeRepository.findById(notice.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("기존 제목");
        assertThat(found.getContent()).isEqualTo("기존 내용");
        assertThat(attachmentIds()).containsExactlyElementsOf(originalAttachmentIds);
        assertFileIds(notice.getId(), List.of(existingFile.getId()));
        assertThat(jdbcTemplate.queryForList("SELECT remind_at FROM notice_reminders WHERE notice_id = ?",
                LocalDateTime.class, notice.getId())).containsExactly(REMIND_AT);
        assertCompleted(existingFile);
    }
}
