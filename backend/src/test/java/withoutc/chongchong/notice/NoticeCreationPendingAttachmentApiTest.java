package withoutc.chongchong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
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
        "notice.attachments.max-count=10",
        "notice.attachments.max-size-bytes=5242880",
        "notice.attachments.allowed-content-types=image/jpeg,image/png,image/webp"
})
class NoticeCreationPendingAttachmentApiTest extends PostgresContainerTest {

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
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private Long leaderId;
    private Long studyId;
    private Long fileId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        User leader = userRepository.save(User.create("리더", null));
        User member = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("검증 대기 공지 스터디", "설명", false));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, member, "스터디원", null, StudyMemberRole.MEMBER));
        FileMetadata file = fileRepository.saveAndFlush(FileMetadata.create(
                leader, "image.png", "uploads/image.png", "image/png", 1024
        ));
        leaderId = leader.getId();
        studyId = study.getId();
        fileId = file.getId();
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("미완료 이미지는 400으로 거부하며 공지와 알림을 저장하지 않는다")
    void rejectPendingAttachment() {
        Response response = request(Map.of("title", "첨부 공지", "content", "공지 내용",
                "remindAts", List.of("2099-01-01T00:00:00"), "attachmentFileIds", List.of(fileId)));

        response.then().statusCode(400)
                .body("code", equalTo("FILE_NOT_ATTACHABLE"))
                .body("message", equalTo("파일을 첨부할 수 없습니다."));
        assertThat(countRows("notices")).isZero();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notice_recipients")).isZero();
        assertThat(countRows("notice_reminders")).isZero();
        assertThat(countRows("notifications")).isZero();
        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.PENDING);
    }

    @Test
    @DisplayName("미완료 파일이 있어도 첨부 없는 공지 생성은 201로 성공한다")
    void createWithoutAttachments() {
        Response response = request(Map.of("title", "첨부 없는 공지", "content", "공지 내용"));

        response.then().statusCode(201);
        assertThat(response.jsonPath().getLong("noticeId")).isNotNull();
        assertThat(countRows("notices")).isOne();
        assertThat(countRows("notice_attachments")).isZero();
        assertThat(countRows("notifications")).isOne();
        assertThat(fileRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileUploadStatus.PENDING);
    }

    private Response request(Object body) {
        return testAuthRequest.givenAuthenticatedUser(leaderId).port(port).contentType(ContentType.JSON)
                .body(body).post("/studies/{studyId}/notices", studyId);
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
