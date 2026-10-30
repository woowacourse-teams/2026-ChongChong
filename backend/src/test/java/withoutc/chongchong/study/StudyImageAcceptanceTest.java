package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.study.controller.dto.StudyCreateRequest;
import withoutc.chongchong.study.controller.dto.StudyUpdateRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3PresignerConfiguration.class)
class StudyImageAcceptanceTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("업로드 완료 이미지 ID로 스터디를 생성하고 기본 정보와 목록에서 이미지를 조회한다")
    void createsStudyWithImage() {
        User owner = userRepository.saveAndFlush(User.create("리더", null));
        FileMetadata image = saveFile(owner, "image/png", true);

        Long studyId = createStudy(owner, image.getId());

        assertThat(imageFileId(studyId)).isEqualTo(image.getId());
        testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .get("/studies/{studyId}/info", studyId)
                .then()
                .statusCode(200)
                .body("imageUrl", containsString(image.getObjectKey()))
                .body("imageUrl", containsString("X-Amz-Signature="));
        testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .get("/studies/me")
                .then()
                .statusCode(200)
                .body("studies[0].imageUrl", containsString(image.getObjectKey()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("이미지 유무와 관계없이 새 이미지로 교체하고 이후 이미지 ID가 null이면 유지한다")
    void replacesAndRetainsImage(boolean hasOriginalImage) {
        User owner = userRepository.saveAndFlush(User.create("리더", null));
        FileMetadata original = hasOriginalImage ? saveFile(owner, "image/png", true) : null;
        Long studyId = createStudy(owner, original == null ? null : original.getId());
        FileMetadata replacement = saveFile(owner, "image/webp", true);

        testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(new StudyUpdateRequest("수정된 이름", "수정된 설명", false, replacement.getId()))
                .patch("/studies/{studyId}", studyId)
                .then()
                .statusCode(204);

        assertThat(imageFileId(studyId)).isEqualTo(replacement.getId());
        assertThat(fileRepository.existsAnyUsageByFileId(replacement.getId())).isTrue();
        if (original != null) {
            assertThat(fileRepository.existsAnyUsageByFileId(original.getId())).isFalse();
            assertThat(fileRepository.findById(original.getId()).orElseThrow().getStatus())
                    .isEqualTo(FileUploadStatus.COMPLETED);
        }
        testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .get("/studies/{studyId}/info", studyId)
                .then()
                .statusCode(200)
                .body("studyName", equalTo("수정된 이름"))
                .body("description", equalTo("수정된 설명"))
                .body("isPublic", equalTo(false))
                .body("imageUrl", containsString(replacement.getObjectKey()));

        testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(new StudyUpdateRequest("다시 수정한 이름", null, null, null))
                .patch("/studies/{studyId}", studyId)
                .then()
                .statusCode(204);

        assertThat(imageFileId(studyId)).isEqualTo(replacement.getId());
        assertThat(studyRepository.getByIdOrThrow(studyId).getName()).isEqualTo("다시 수정한 이름");
    }

    @Test
    @DisplayName("타인 이미지로 생성을 요청하면 403을 반환하고 스터디와 멤버를 저장하지 않는다")
    void rejectsCreationWithAnotherUsersImage() {
        User requester = userRepository.saveAndFlush(User.create("요청자", null));
        User owner = userRepository.saveAndFlush(User.create("파일 소유자", null));
        FileMetadata image = saveFile(owner, "image/png", true);

        testAuthRequest.givenAuthenticatedUser(requester.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(new StudyCreateRequest("새 스터디", "설명", true, image.getId()))
                .post("/studies")
                .then()
                .statusCode(403)
                .body("code", equalTo("FILE_NOT_OWNED"));

        assertThat(studyRepository.findAll()).isEmpty();
        assertThat(studyMemberRepository.findAll()).isEmpty();
        assertThat(fileRepository.existsAnyUsageByFileId(image.getId())).isFalse();
        assertThat(fileRepository.findById(image.getId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("미완료 파일로 이미지 교체를 요청하면 스터디 정보와 기존 이미지를 유지한다")
    void preservesStudyWhenReplacementIsNotCompleted() {
        User owner = userRepository.saveAndFlush(User.create("리더", null));
        FileMetadata original = saveFile(owner, "image/png", true);
        Long studyId = createStudy(owner, original.getId());
        FileMetadata pending = saveFile(owner, "image/png", false);

        testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(new StudyUpdateRequest("거절할 이름", "거절할 설명", false, pending.getId()))
                .patch("/studies/{studyId}", studyId)
                .then()
                .statusCode(400)
                .body("code", equalTo("FILE_NOT_ATTACHABLE"));

        Study unchanged = studyRepository.getByIdOrThrow(studyId);
        assertThat(unchanged.getName()).isEqualTo("원래 이름");
        assertThat(unchanged.getDescription()).isEqualTo("원래 설명");
        assertThat(unchanged.isPublic()).isTrue();
        assertThat(imageFileId(studyId)).isEqualTo(original.getId());
        assertThat(fileRepository.existsAnyUsageByFileId(original.getId())).isTrue();
        assertThat(fileRepository.existsAnyUsageByFileId(pending.getId())).isFalse();
        assertThat(fileRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.PENDING);
    }

    @Test
    @DisplayName("완료된 파일도 스터디 이미지 허용 형식이 아니면 생성 요청을 거절한다")
    void rejectsCreationWithUnsupportedImageType() {
        User owner = userRepository.saveAndFlush(User.create("리더", null));
        FileMetadata file = saveFile(owner, "application/pdf", true);

        testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(new StudyCreateRequest("새 스터디", "설명", true, file.getId()))
                .post("/studies")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_IMAGE_TYPE"));

        assertThat(studyRepository.findAll()).isEmpty();
        assertThat(studyMemberRepository.findAll()).isEmpty();
        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isFalse();
        assertThat(fileRepository.findById(file.getId()).orElseThrow().getStatus())
                .isEqualTo(FileUploadStatus.COMPLETED);
    }

    private Long createStudy(User owner, Long imageFileId) {
        return testAuthRequest.givenAuthenticatedUser(owner.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(new StudyCreateRequest("원래 이름", "원래 설명", true, imageFileId))
                .post("/studies")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("studyId");
    }

    private FileMetadata saveFile(User owner, String contentType, boolean completed) {
        FileMetadata file = FileMetadata.create(owner, "image", "study-images/" + UUID.randomUUID(), contentType, 1024);
        if (completed) {
            file.completeUpload();
        }
        return fileRepository.saveAndFlush(file);
    }

    private Long imageFileId(Long studyId) {
        return jdbcTemplate.queryForObject("SELECT image_file_id FROM studies WHERE id = ?", Long.class, studyId);
    }
}
