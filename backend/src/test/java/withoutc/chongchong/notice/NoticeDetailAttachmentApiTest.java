package withoutc.chongchong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeDetailAttachmentApiTest extends PostgresContainerTest {

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

    @LocalServerPort
    private int port;

    private Long studyId;
    private Long noticeId;
    private Long memberId;
    private Long fileOwnerId;
    private Long firstFileId;
    private Long secondFileId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        User leader = userRepository.save(User.create("현재 리더", null));
        User formerMember = userRepository.save(User.create("기존 첨부 업로더", null));
        Study study = studyRepository.save(Study.create("공지 상세 스터디", "설명"));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        FileMetadata firstFile = saveCompletedFile(formerMember, "first.png", "image/png", 1024);
        FileMetadata secondFile = saveCompletedFile(formerMember, "second.webp", "image/webp", 2048);
        Notice notice = Notice.create(study, "이미지 첨부 공지", "공지 내용");
        notice.addAttachments(List.of(secondFile, firstFile));
        noticeRepository.saveAndFlush(notice);

        User member = userRepository.save(User.create("공지 작성 후 가입한 스터디원", null));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        studyId = study.getId();
        noticeId = notice.getId();
        memberId = member.getId();
        fileOwnerId = formerMember.getId();
        firstFileId = firstFile.getId();
        secondFileId = secondFile.getId();
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("기존 첨부 메타데이터를 표시 순서로 반환한다")
    void getAttachmentsInDisplayOrder() {
        Response response = request(memberId);

        response.then().statusCode(200)
                .body("id", equalTo(noticeId.intValue()))
                .body("attachments.fileId", contains(secondFileId.intValue(), firstFileId.intValue()))
                .body("attachments.originalFileName", contains("second.webp", "first.png"))
                .body("attachments.contentType", contains("image/webp", "image/png"))
                .body("attachments.size", contains(2048, 1024))
                .body("attachments.sortOrder", contains(0, 1));
        List<Map<String, Object>> attachments = response.jsonPath().getList("attachments");
        assertThat(attachments).allSatisfy(attachment -> assertThat(attachment)
                .containsOnlyKeys("fileId", "originalFileName", "contentType", "size", "sortOrder"));
        assertThat(fileRepository.findById(firstFileId).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
        assertThat(fileRepository.findById(secondFileId).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("공지 작성 후 가입한 스터디원도 첨부를 조회하며 수신자로 추가되지 않는다")
    void readOlderNoticeWithoutAddingRecipient() {
        assertThat(countRecipients()).isZero();

        request(memberId).then().statusCode(200)
                .body("attachments.fileId", contains(secondFileId.intValue(), firstFileId.intValue()));

        assertThat(countRecipients()).isZero();
    }

    @Test
    @DisplayName("첨부 파일 소유자여도 현재 스터디원이 아니면 공지 첨부를 조회할 수 없다")
    void rejectFileOwnerOutsideStudy() {
        request(fileOwnerId).then().statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 공지 첨부를 조회할 수 없다")
    void rejectAnonymousUser() {
        RestAssured.given().port(port).basePath(API_PREFIX)
                .get("/studies/{studyId}/notices/{noticeId}", studyId, noticeId)
                .then().statusCode(401);
    }

    private Response request(Long userId) {
        return testAuthRequest.givenAuthenticatedUser(userId).port(port)
                .get("/studies/{studyId}/notices/{noticeId}", studyId, noticeId);
    }

    private FileMetadata saveCompletedFile(User owner, String name, String contentType, long size) {
        FileMetadata file = FileMetadata.create(owner, name, "uploads/" + name, contentType, size);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private Long countRecipients() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notice_recipients WHERE notice_id = ?", Long.class, noticeId
        );
    }
}
