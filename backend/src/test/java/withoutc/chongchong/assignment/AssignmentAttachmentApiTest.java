package withoutc.chongchong.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.S3FileMetadataReader;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Import(AssignmentAttachmentApiTest.SigningConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssignmentAttachmentApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private FileMetaDataRepository fileMetaDataRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @MockitoBean
    private S3FileMetadataReader s3FileMetadataReader;

    @LocalServerPort
    private int port;

    private User leader;
    private User member;
    private User other;
    private User outsider;
    private Study study;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.save(User.create("리더", null));
        member = userRepository.save(User.create("제출자", null));
        other = userRepository.save(User.create("다른 멤버", null));
        outsider = userRepository.save(User.create("외부인", null));
        study = studyRepository.save(Study.create("파일 과제", "설명", false));
        studyMemberRepository.save(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, member, "제출자", null, StudyMemberRole.MEMBER));
        studyMemberRepository.save(StudyMember.create(study, other, "다른 멤버", null, StudyMemberRole.MEMBER));
    }

    @Test
    @DisplayName("과제 첨부를 등록하고 순서를 변경하거나 제거할 수 있다")
    void createsReordersRemovesAndDeletesAssignmentFiles() {
        Long first = file(leader, "application/pdf", 1024, true);
        Long second = file(leader, "image/png", 1024, true);
        Long assignment = create(List.of(first, second));
        Response detail = get(member, path(assignment));
        detail.then().statusCode(200);
        assertThat(detail.jsonPath().getList("attachments.fileId", Long.class)).containsExactly(first, second);
        patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(second, first))).then().statusCode(204);
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments.fileId", Long.class))
                .containsExactly(second, first);
        patch(leader, path(assignment), Map.of("title", "제목만 수정")).then().statusCode(204);
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments.fileId", Long.class)).hasSize(2);
        patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(second))).then().statusCode(204);
        assertStatus(first, FileUploadStatus.COMPLETED);
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments.fileId", Long.class))
                .containsExactly(second);
        patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(second, first))).then().statusCode(204);
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments.fileId", Long.class))
                .containsExactly(second, first);
        testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port).delete(path(assignment)).then().statusCode(204);
        assertStatus(second, FileUploadStatus.COMPLETED);
        assertThat(fileMetaDataRepository.existsAssignmentAttachmentByFileId(first)).isFalse();
        assertThat(fileMetaDataRepository.existsAssignmentAttachmentByFileId(second)).isFalse();
    }

    @Test
    @DisplayName("업로드 완료 후 과제와 제출물에 파일을 연결할 수 있다")
    void connectsFilesToAssignmentAndSubmissionAfterUploadCompletion() {
        Long assignment = create(List.of());
        Long material = file(leader, "application/pdf", 1024, false);
        patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(material))).then().statusCode(400);

        when(s3FileMetadataReader.findSize(fileMetaDataRepository.findById(material).orElseThrow().getObjectKey()))
                .thenReturn(OptionalLong.of(1024));
        testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port)
                .post("/files/" + material + "/complete").then().statusCode(204);
        patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(material))).then().statusCode(204);
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments.fileId", Long.class))
                .containsExactly(material);

        Long result = file(member, "application/pdf", 2048, false);
        post(member, path(assignment) + "/submissions", Map.of("attachmentFileIds", List.of(result)))
                .then().statusCode(400);
        when(s3FileMetadataReader.findSize(fileMetaDataRepository.findById(result).orElseThrow().getObjectKey()))
                .thenReturn(OptionalLong.of(2048));
        testAuthRequest.givenAuthenticatedUser(member.getId()).port(port)
                .post("/files/" + result + "/complete").then().statusCode(204);
        post(member, path(assignment) + "/submissions", Map.of("attachmentFileIds", List.of(result)))
                .then().statusCode(201);
        assertThat(get(member, path(assignment) + "/submissions/my")
                .jsonPath().getList("attachments.fileId", Long.class)).containsExactly(result);
        assertStatus(material, FileUploadStatus.COMPLETED);
        assertStatus(result, FileUploadStatus.COMPLETED);
    }

    @Test
    @DisplayName("첨부파일의 소유권과 상태 및 제한을 검증한다")
    void validatesOwnershipStateTypeSizeCountAndDuplicateIds() {
        Long assignment = create(List.of());
        for (Long id : List.of(file(member, "image/png", 1, true), file(leader, "image/png", 1, false),
                file(leader, "text/html", 1, true), file(leader, "application/pdf", 5242881, true))) {
            Response response = patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(id)));
            assertThat(response.statusCode()).isIn(400, 403);
        }
        Long own = file(leader, "image/png", 1, true);
        patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(own, own))).then().statusCode(400);
        patch(leader, path(assignment), Map.of("attachmentFileIds", Collections.nCopies(11, own)))
                .then().statusCode(400);
        patch(member, path(assignment), Map.of("attachmentFileIds", List.of())).then().statusCode(403);
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments")).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    @DisplayName("첨부파일 ID가 null이거나 양수가 아니면 생성과 수정을 거부한다")
    void rejectsInvalidAttachmentElementsInEveryWriteRequest(Long invalidId) {
        List<Long> ids = Collections.singletonList(invalidId);
        Response creation = post(leader, "/studies/" + study.getId() + "/assignments", Map.of(
                "title", "과제", "content", "안내", "submissionTarget", "MEMBERS_ONLY",
                "submissionVisibility", "LEADER_ONLY", "closeAt", "2099-12-31T23:59:00",
                "attachmentFileIds", ids));
        Long assignment = create(List.of());
        Response update = patch(leader, path(assignment), Map.of("attachmentFileIds", ids));
        Response submission = post(member, path(assignment) + "/submissions",
                Map.of("content", "제출", "attachmentFileIds", ids));
        Response validSubmission = post(member, path(assignment) + "/submissions", Map.of("content", "기존 내용"));
        validSubmission.then().statusCode(201);
        Long submissionId = validSubmission.jsonPath().getLong("submissionId");
        Response submissionUpdate = patch(member, path(assignment) + "/submissions/" + submissionId,
                Map.of("attachmentFileIds", ids));

        for (Response response : List.of(creation, update, submission, submissionUpdate)) {
            response.then().statusCode(400);
            assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_INPUT_VALUE");
        }
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments")).isEmpty();
        assertThat(get(member, path(assignment) + "/submissions/my").jsonPath().getString("content"))
                .isEqualTo("기존 내용");
    }

    @Test
    @DisplayName("텍스트와 파일을 제출하고 공개 범위에 따라 첨부 정보를 조회한다")
    void allowsMixedSubmissionAndEnforcesAttachmentVisibility() {
        Long assignment = create(List.of());
        post(member, path(assignment) + "/submissions", Map.of("content", "텍스트" )).then().statusCode(201);
        Long pdf = file(member, "application/pdf", 10, true);
        Response submitted = post(member, path(assignment) + "/submissions", Map.of("attachmentFileIds", List.of(pdf)));
        submitted.then().statusCode(201);
        Long submission = submitted.jsonPath().getLong("submissionId");
        String submissionPath = path(assignment) + "/submissions/" + submission;
        get(member, path(assignment) + "/submissions/my").then().statusCode(200);
        get(other, submissionPath).then().statusCode(403);
        get(outsider, submissionPath).then().statusCode(403);
        assertThat(get(leader, submissionPath).jsonPath().getList("attachments.fileId", Long.class))
                .containsExactly(pdf);
        patch(leader, path(assignment), Map.of("submissionVisibility", "ALL_STUDY_MEMBERS")).then().statusCode(204);
        assertThat(get(other, submissionPath).jsonPath().getList("attachments.fileId", Long.class)).containsExactly(pdf);
        assertThat(get(member, submissionPath).jsonPath().getList("attachments.fileId", Long.class)).containsExactly(pdf);
        patch(member, submissionPath, Map.of("content", "새 텍스트")).then().statusCode(204);
        patch(member, submissionPath, Map.of("content", "새 텍스트", "attachmentFileIds", List.of()))
                .then().statusCode(204);
        assertStatus(pdf, FileUploadStatus.COMPLETED);
        assertThat(get(member, submissionPath).jsonPath().getList("attachments")).isEmpty();
    }

    @Test
    @DisplayName("동시에 같은 파일을 연결하면 하나의 과제만 성공한다")
    void concurrentAssignmentsCannotClaimTheSameFile() throws Exception {
        Long first = create(List.of());
        Long second = create(List.of());
        Long shared = file(leader, "application/pdf", 1, true);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return patch(leader, path(first), Map.of("attachmentFileIds", List.of(shared))).statusCode();
            });
            var b = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return patch(leader, path(second), Map.of("attachmentFileIds", List.of(shared))).statusCode();
            });
            assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204, 400);
        }
    }

    @Test
    @DisplayName("과제를 삭제해도 제출물 파일은 완료 상태로 유지하고 연결만 제거한다")
    void deletingAssignmentUnlinksSubmissionFiles() {
        Long assignment = create(List.of());
        Long image = file(member, "image/png", 10, true);
        post(member, path(assignment) + "/submissions", Map.of("attachmentFileIds", List.of(image)))
                .then().statusCode(201);
        testAuthRequest.givenAuthenticatedUser(leader.getId()).port(port).delete(path(assignment)).then().statusCode(204);
        assertStatus(image, FileUploadStatus.COMPLETED);
        assertThat(fileMetaDataRepository.existsSubmissionAttachmentByFileId(image)).isFalse();
    }

    @Test
    @DisplayName("제출물 첨부를 교체해도 최초 제출 시각은 유지한다")
    void replacesSubmissionAttachmentsAndPreservesFirstSubmissionTime() {
        Long assignment = create(List.of());
        Long first = file(member, "application/pdf", 1, true);
        Long second = file(member, "application/zip", 1, true);
        Long third = file(member, "text/plain", 1, true);
        Response submitted = post(member, path(assignment) + "/submissions",
                Map.of("content", "설명", "attachmentFileIds", List.of(first, second)));
        submitted.then().statusCode(201);
        String submissionPath = path(assignment) + "/submissions/" + submitted.jsonPath().getLong("submissionId");
        String firstSubmittedAt = get(member, submissionPath).jsonPath().getString("createdAt");
        patch(member, submissionPath, Map.of("attachmentFileIds", List.of(second, first))).then().statusCode(204);
        assertThat(get(member, submissionPath).jsonPath().getList("attachments.fileId", Long.class))
                .containsExactly(second, first);
        patch(member, submissionPath, Map.of("attachmentFileIds", List.of(second, third))).then().statusCode(204);
        assertStatus(first, FileUploadStatus.COMPLETED);
        patch(member, submissionPath, Map.of("content", "설명 변경")).then().statusCode(204);
        Response detail = get(member, submissionPath);
        assertThat(detail.jsonPath().getList("attachments.fileId", Long.class)).containsExactly(second, third);
        assertThat(detail.jsonPath().getString("createdAt")).isEqualTo(firstSubmittedAt);
        patch(other, submissionPath, Map.of("attachmentFileIds", List.of())).then().statusCode(403);
    }

    @Test
    @DisplayName("빈 제출과 과제에 연결된 파일의 재사용을 거부한다")
    void rejectsEmptySubmissionAndReuseAcrossAssignmentAndSubmission() {
        Long assignment = create(List.of());
        post(member, path(assignment) + "/submissions", Map.of()).then().statusCode(400);
        patch(leader, path(assignment), Map.of("submissionTarget", "MEMBERS_AND_LEADER")).then().statusCode(204);
        Long fileId = file(leader, "application/pdf", 1, true);
        patch(leader, path(assignment), Map.of("attachmentFileIds", List.of(fileId))).then().statusCode(204);
        post(leader, path(assignment) + "/submissions", Map.of("attachmentFileIds", List.of(fileId)))
                .then().statusCode(400);
    }

    @Test
    @DisplayName("리더를 제출 대상에서 제외하면 파일 상태를 유지하고 연결만 제거한다")
    void removingLeaderFromSubmissionTargetUnlinksTheirFiles() {
        Long assignment = create(List.of());
        patch(leader, path(assignment), Map.of("submissionTarget", "MEMBERS_AND_LEADER")).then().statusCode(204);
        Long fileId = file(leader, "application/pdf", 1, true);
        post(leader, path(assignment) + "/submissions", Map.of("attachmentFileIds", List.of(fileId)))
                .then().statusCode(201);
        patch(leader, path(assignment), Map.of("submissionTarget", "MEMBERS_ONLY")).then().statusCode(204);
        assertStatus(fileId, FileUploadStatus.COMPLETED);
        assertThat(fileMetaDataRepository.existsSubmissionAttachmentByFileId(fileId)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    })
    @DisplayName("DOCX와 PPTX를 과제와 제출물에 첨부할 수 있다")
    void attachesOfficeDocuments(String contentType) {
        Long assignmentFile = file(leader, contentType, 1024, true);
        Long assignment = create(List.of(assignmentFile));
        assertThat(get(member, path(assignment)).jsonPath().getList("attachments.contentType", String.class))
                .containsExactly(contentType);
        Long submissionFile = file(member, contentType, 1024, true);
        post(member, path(assignment) + "/submissions", Map.of("attachmentFileIds", List.of(submissionFile)))
                .then().statusCode(201);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("조회 권한이 있는 사용자에게만 이미지 URL을 반환하고 문서는 URL 없이 표시한다")
    void returnsImageUrlsAfterAccessCheck(String contentType) {
        Long image = file(leader, contentType, 1024, true);
        Long document = file(leader, "application/pdf", 1024, true);
        Long assignment = create(List.of(image, document));
        Response detail = get(member, path(assignment));
        detail.then().statusCode(200);
        URI url = URI.create(detail.jsonPath().getString("attachments[0].imageUrl"));
        assertThat(url.getScheme()).isEqualTo("https");
        assertThat(url.getPath()).isEqualTo("/" + fileMetaDataRepository.findById(image).orElseThrow().getObjectKey());
        assertThat(url.getQuery()).contains("X-Amz-Signature=", "X-Amz-Expires=", "response-content-disposition=inline",
                "response-content-type=" + contentType);
        assertThat(detail.jsonPath().getString("attachments[1].imageUrl")).isNull();
        get(outsider, path(assignment)).then().statusCode(403);

        Long submissionImage = file(member, contentType, 1024, true);
        Response submitted = post(member, path(assignment) + "/submissions",
                Map.of("attachmentFileIds", List.of(submissionImage)));
        submitted.then().statusCode(201);
        String submissionPath = path(assignment) + "/submissions/" + submitted.jsonPath().getLong("submissionId");
        assertThat(get(member, path(assignment) + "/submissions/my").jsonPath()
                .getString("attachments[0].imageUrl")).contains("X-Amz-Signature=");
        assertThat(get(leader, submissionPath).jsonPath().getString("attachments[0].imageUrl"))
                .contains("X-Amz-Signature=");
        Response forbidden = get(other, submissionPath);
        forbidden.then().statusCode(403);
        assertThat(forbidden.asString()).doesNotContain("X-Amz-Signature");
        patch(leader, path(assignment), Map.of("submissionVisibility", "ALL_STUDY_MEMBERS")).then().statusCode(204);
        assertThat(get(other, submissionPath).jsonPath().getString("attachments[0].imageUrl"))
                .contains("X-Amz-Signature=");
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"})
    @DisplayName("과제와 제출물의 다운로드 URL은 기존 조회 권한에 따라 반환한다")
    void returnsDownloadUrlsAfterAccessCheck(String contentType) {
        Long material = file(leader, contentType, 1024, true);
        Long assignment = create(List.of(material));
        Response detail = get(member, path(assignment));
        detail.then().statusCode(200);
        assertThat(detail.jsonPath().getString("attachments[0].downloadUrl"))
                .contains("X-Amz-Signature=", "response-content-disposition=attachment");
        get(outsider, path(assignment)).then().statusCode(403);

        Long result = file(member, contentType, 1024, true);
        Response submitted = post(member, path(assignment) + "/submissions",
                Map.of("attachmentFileIds", List.of(result)));
        submitted.then().statusCode(201);
        String submissionPath = path(assignment) + "/submissions/" + submitted.jsonPath().getLong("submissionId");
        for (User allowed : List.of(member, leader)) {
            assertThat(get(allowed, submissionPath).jsonPath().getString("attachments[0].downloadUrl"))
                    .contains("X-Amz-Signature=");
        }
        assertThat(get(member, path(assignment) + "/submissions/my").jsonPath()
                .getString("attachments[0].downloadUrl")).contains("X-Amz-Signature=");
        Response forbidden = get(other, submissionPath);
        forbidden.then().statusCode(403);
        assertThat(forbidden.asString()).doesNotContain("downloadUrl", "X-Amz-Signature");
        patch(leader, path(assignment), Map.of("submissionVisibility", "ALL_STUDY_MEMBERS")).then().statusCode(204);
        assertThat(get(other, submissionPath).jsonPath().getString("attachments[0].downloadUrl"))
                .contains("X-Amz-Signature=");
        get(outsider, submissionPath).then().statusCode(403);
    }

    private Long create(List<Long> attachments) {
        Response response = post(leader, "/studies/" + study.getId() + "/assignments", Map.of(
                "title", "과제", "content", "안내", "submissionTarget", "MEMBERS_ONLY",
                "submissionVisibility", "LEADER_ONLY", "closeAt", "2099-12-31T23:59:00",
                "attachmentFileIds", attachments));
        response.then().statusCode(201);
        return response.jsonPath().getLong("assignmentId");
    }

    private Long file(User owner, String type, long size, boolean completed) {
        FileMetadata file = FileMetadata.create(owner, "file", "test/" + UUID.randomUUID(), type, size);
        if (completed) {
            file.completeUpload();
        }
        return fileMetaDataRepository.saveAndFlush(file).getId();
    }

    private String path(Long assignment) {
        return "/studies/" + study.getId() + "/assignments/" + assignment;
    }

    private Response post(User user, String path, Object body) {
        return testAuthRequest.givenAuthenticatedUser(user.getId()).port(port).contentType(ContentType.JSON).body(body).post(path);
    }

    private Response patch(User user, String path, Object body) {
        return testAuthRequest.givenAuthenticatedUser(user.getId()).port(port).contentType(ContentType.JSON).body(body).patch(path);
    }

    private Response get(User user, String path) {
        return testAuthRequest.givenAuthenticatedUser(user.getId()).port(port).get(path);
    }

    private void assertStatus(Long fileId, FileUploadStatus status) {
        assertThat(fileMetaDataRepository.findById(fileId).orElseThrow().getStatus()).isEqualTo(status);
    }

    @TestConfiguration
    static class SigningConfiguration {

        @Bean
        @Primary
        S3Presigner testPresigner() {
            return S3Presigner.builder()
                    .region(Region.AP_NORTHEAST_2)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                    .build();
        }
    }

}
