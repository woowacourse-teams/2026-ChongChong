package withoutc.chongchong.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUploadTransactionService;
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
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.service.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3PresignerConfiguration.class)
class UserWithdrawalFileApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private FileUploadTransactionService uploadTransactionService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestAuthRequest auth;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private Clock clock;

    @MockitoBean
    private S3FileMetadataReader metadataReader;

    @LocalServerPort
    private int port;

    private User owner;
    private User other;
    private FileMetadata profile;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        owner = userRepository.saveAndFlush(User.create("탈퇴자", "https://example.com/social.png"));
        other = userRepository.saveAndFlush(User.create("다른 사용자", null));
        profile = saveFile(owner, FileUploadStatus.COMPLETED);
        owner.changeProfileImage(profile);
        userRepository.saveAndFlush(owner);
    }

    @AfterEach
    void cleanDatabase() {
        cleaner.clean();
    }

    @ParameterizedTest
    @EnumSource(FileUploadStatus.class)
    @DisplayName("업로드 파일이 있어도 탈퇴할 수 있고 파일 상태를 보존하며 본인의 소유자 연결만 해제한다")
    void detachesOwnerWithoutDeletingFiles(FileUploadStatus status) {
        FileMetadata file = saveFile(owner, status);
        FileMetadata otherFile = saveFile(other, FileUploadStatus.COMPLETED);

        Response response = withdraw(owner.getId());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body().asByteArray()).isEmpty();
        assertThat(userRepository.existsById(owner.getId())).isFalse();
        assertDetached(profile, FileUploadStatus.COMPLETED, false);
        assertDetached(file, status, false);
        assertThat(fileRepository.getUploadSnapshotByIdOrThrow(otherFile.getId()).ownerId()).isEqualTo(other.getId());
        assertThat(userRepository.existsById(other.getId())).isTrue();
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("탈퇴 후 공지·과제·스터디 이미지와 파일은 보존하고 본인 제출물·프로필 연결만 제거한다")
    void preservesResourceFilesAfterWithdrawal() {
        Study study = createStudy();
        FileMetadata cover = saveFile(owner, FileUploadStatus.COMPLETED);
        study.changeImage(cover);
        studyRepository.saveAndFlush(study);
        StudyMember member = memberRepository.saveAndFlush(StudyMember.create(
                study, owner, owner.getName(), null, StudyMemberRole.MEMBER));
        FileMetadata noticeFile = saveFile(owner, FileUploadStatus.COMPLETED);
        Notice notice = Notice.create(study, "공지", "내용");
        notice.addAttachments(List.of(noticeFile));
        noticeRepository.saveAndFlush(notice);
        FileMetadata assignmentFile = saveFile(owner, FileUploadStatus.COMPLETED);
        LocalDateTime now = LocalDateTime.now(clock);
        Assignment assignment = Assignment.create(study, "과제", "내용", "링크", SubmissionTarget.MEMBERS_ONLY,
                SubmissionVisibility.LEADER_ONLY, now.plusDays(1), now);
        assignment.addAttachments(List.of(assignmentFile));
        assignmentRepository.saveAndFlush(assignment);
        FileMetadata submissionFile = saveFile(owner, FileUploadStatus.COMPLETED);
        AssignmentSubmission submission = AssignmentSubmission.create(member, assignment);
        submission.addAttachments(List.of(submissionFile));
        submissionRepository.saveAndFlush(submission);

        assertThat(withdraw(owner.getId()).statusCode()).isEqualTo(204);

        assertThat(memberRepository.existsById(member.getId())).isFalse();
        assertThat(submissionRepository.existsById(submission.getId())).isFalse();
        assertThat(noticeRepository.existsById(notice.getId())).isTrue();
        assertThat(assignmentRepository.existsById(assignment.getId())).isTrue();
        assertDetached(profile, FileUploadStatus.COMPLETED, false);
        assertDetached(submissionFile, FileUploadStatus.COMPLETED, false);
        assertDetached(noticeFile, FileUploadStatus.COMPLETED, true);
        assertDetached(assignmentFile, FileUploadStatus.COMPLETED, true);
        assertDetached(cover, FileUploadStatus.COMPLETED, true);
        assertReadableAttachment("/studies/" + study.getId() + "/notices/" + notice.getId(), noticeFile);
        assertReadableAttachment("/studies/" + study.getId() + "/assignments/" + assignment.getId(), assignmentFile);
        verifyNoInteractions(metadataReader);
    }

    @ParameterizedTest
    @EnumSource(FileUploadStatus.class)
    @DisplayName("소유자 없는 파일은 snapshot 조회에서 사라지지 않고 완료의 두 검증 단계 모두 소유권 오류로 거절한다")
    void rejectsCompletingOwnerlessFile(FileUploadStatus status) {
        FileMetadata file = saveFile(owner, status);
        assertThat(withdraw(owner.getId()).statusCode()).isEqualTo(204);

        assertThat(fileRepository.getUploadSnapshotByIdOrThrow(file.getId()).ownerId()).isNull();
        assertForbidden(auth.givenAuthenticatedUser(other.getId()).port(port)
                .post("/files/" + file.getId() + "/complete"));
        assertThatThrownBy(() -> uploadTransactionService.complete(other.getId(), file.getId(), file.getSize()))
                .isInstanceOfSatisfying(FileException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_OWNED));
        assertDetached(file, status, false);
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("S3 검증 중 소유자가 탈퇴해도 완료 저장 시 재검증해 NPE 대신 소유권 오류를 반환한다")
    void rejectsCompletionWhenOwnerWithdrawsDuringS3Read() {
        FileMetadata file = saveFile(owner, FileUploadStatus.PENDING);
        when(metadataReader.findSize(file.getObjectKey())).thenAnswer(invocation -> {
            userService.withdraw(owner.getId());
            return OptionalLong.of(file.getSize());
        });

        assertForbidden(auth.givenAuthenticatedUser(owner.getId()).port(port)
                .post("/files/" + file.getId() + "/complete"));

        assertThat(userRepository.existsById(owner.getId())).isFalse();
        assertDetached(file, FileUploadStatus.PENDING, false);
    }

    @Test
    @DisplayName("소유자 없는 완료 파일은 다른 사용자의 홈 프로필이나 새 공지에 연결할 수 없다")
    void rejectsAttachingOwnerlessFile() {
        Study study = createStudy();
        assertThat(withdraw(owner.getId()).statusCode()).isEqualTo(204);

        assertForbidden(auth.givenAuthenticatedUser(other.getId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("fileId", profile.getId())).patch("/users/me/profile-image"));
        assertForbidden(auth.givenAuthenticatedUser(other.getId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("title", "공지", "content", "내용", "attachmentFileIds", List.of(profile.getId())))
                .post("/studies/" + study.getId() + "/notices"));

        assertThat(userRepository.findById(other.getId()).orElseThrow().getProfileImageFile()).isNull();
        assertThat(noticeRepository.count()).isZero();
        assertDetached(profile, FileUploadStatus.COMPLETED, false);
        verifyNoInteractions(metadataReader);
    }

    @Test
    @DisplayName("리더의 탈퇴가 차단되면 사용자와 프로필·파일 소유권을 모두 유지한다")
    void preservesOwnerWhenWithdrawalIsBlocked() {
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명", false));
        memberRepository.saveAndFlush(StudyMember.create(study, owner, owner.getName(), null, StudyMemberRole.LEADER));

        Response response = withdraw(owner.getId());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("code")).isEqualTo("STUDY_LEADER_WITHDRAWAL_BLOCKED");
        assertThat(userRepository.existsById(owner.getId())).isTrue();
        assertThat(fileRepository.getUploadSnapshotByIdOrThrow(profile.getId()).ownerId()).isEqualTo(owner.getId());
        assertThat(fileRepository.existsAnyUsageByFileId(profile.getId())).isTrue();
        verifyNoInteractions(metadataReader);
    }

    private Study createStudy() {
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명", false));
        memberRepository.saveAndFlush(StudyMember.create(study, other, other.getName(), null, StudyMemberRole.LEADER));
        return study;
    }

    private FileMetadata saveFile(User user, FileUploadStatus status) {
        FileMetadata file = FileMetadata.create(user, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        if (status != FileUploadStatus.PENDING) {
            file.completeUpload();
        }
        if (status == FileUploadStatus.DELETE_PENDING) {
            file.markForDeletion();
        }
        return fileRepository.saveAndFlush(file);
    }

    private Response withdraw(Long userId) {
        return auth.givenAuthenticatedUser(userId).port(port).delete("/users/me");
    }

    private void assertDetached(FileMetadata original, FileUploadStatus status, boolean used) {
        FileMetadata saved = fileRepository.findById(original.getId()).orElseThrow();
        assertThat(saved.getOwner()).isNull();
        assertThat(saved.getStatus()).isEqualTo(status);
        assertThat(saved.getObjectKey()).isEqualTo(original.getObjectKey());
        assertThat(saved.isOwnedBy(other.getId())).isFalse();
        assertThat(fileRepository.existsAnyUsageByFileId(saved.getId())).isEqualTo(used);
    }

    private void assertForbidden(Response response) {
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.jsonPath().getString("code")).isEqualTo("FILE_NOT_OWNED");
    }

    private void assertReadableAttachment(String path, FileMetadata file) {
        Response response = auth.givenAuthenticatedUser(other.getId()).port(port).get(path);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getLong("attachments[0].fileId")).isEqualTo(file.getId());
        assertThat(response.jsonPath().getString("attachments[0].imageUrl"))
                .contains(file.getObjectKey(), "X-Amz-Signature=");
    }
}
