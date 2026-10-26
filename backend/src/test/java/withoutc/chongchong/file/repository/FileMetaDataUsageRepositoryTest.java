package withoutc.chongchong.file.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;

@Transactional
class FileMetaDataUsageRepositoryTest extends PostgresContainerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(FileUploadStatus.class)
    @DisplayName("첨부 관계가 없으면 업로드 상태와 무관하게 미연결로 조회한다")
    void findUnlinkedFileRegardlessOfUploadStatus(FileUploadStatus status) {
        FileMetadata file = saveFile(status);
        entityManager.flush();

        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 파일은 첨부 관계가 없는 것으로 조회한다")
    void findMissingFileAsUnlinked() {
        assertThat(fileRepository.existsAnyUsageByFileId(Long.MAX_VALUE)).isFalse();
    }

    @Test
    @DisplayName("공지에 연결된 파일만 사용 중이며 연결 제거 후에는 미연결로 조회한다")
    void findCurrentNoticeAttachment() {
        FileMetadata file = saveFile(FileUploadStatus.COMPLETED);
        Notice notice = Notice.create(saveStudy(), "공지 제목", "공지 내용");
        notice.addAttachments(List.of(file));
        entityManager.persist(notice);
        entityManager.flush();

        assertOnlyRequestedFileLinked(file);

        notice.clearAttachments();
        assertUnlinkedAfterFlush(file);
    }

    @Test
    @DisplayName("과제에 연결된 파일만 사용 중이며 연결 제거 후에는 미연결로 조회한다")
    void findCurrentAssignmentAttachment() {
        FileMetadata file = saveFile(FileUploadStatus.COMPLETED);
        Assignment assignment = saveAssignment(saveStudy());
        assignment.addAttachments(List.of(file));
        entityManager.flush();

        assertOnlyRequestedFileLinked(file);

        assignment.clearAttachments();
        assertUnlinkedAfterFlush(file);
    }

    @Test
    @DisplayName("제출물에 연결된 파일만 사용 중이며 연결 제거 후에는 미연결로 조회한다")
    void findCurrentSubmissionAttachment() {
        FileMetadata file = saveFile(FileUploadStatus.COMPLETED);
        Study study = saveStudy();
        Assignment assignment = saveAssignment(study);
        StudyMember member = StudyMember.create(study, file.getOwner(), "제출자", null, StudyMemberRole.MEMBER);
        entityManager.persist(member);
        AssignmentSubmission submission = AssignmentSubmission.create(member, assignment);
        submission.addAttachments(List.of(file));
        entityManager.persist(submission);
        entityManager.flush();

        assertOnlyRequestedFileLinked(file);

        submission.clearAttachments();
        assertUnlinkedAfterFlush(file);
    }

    @Test
    @DisplayName("스터디 이미지에 연결된 파일은 공지와 과제 첨부에 재사용할 수 없다")
    void findCurrentStudyImage() {
        FileMetadata file = saveFile(FileUploadStatus.COMPLETED);
        Study study = saveStudy();
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE studies SET image_file_id = :fileId WHERE id = :studyId")
                .setParameter("fileId", file.getId())
                .setParameter("studyId", study.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isTrue();
        assertThat(fileRepository.existsStudyImageByFileId(file.getId())).isTrue();

        entityManager.createNativeQuery("UPDATE studies SET image_file_id = NULL WHERE id = :studyId")
                .setParameter("studyId", study.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isFalse();
        assertThat(fileRepository.existsStudyImageByFileId(file.getId())).isFalse();
    }

    private void assertOnlyRequestedFileLinked(FileMetadata linkedFile) {
        FileMetadata unlinkedFile = saveFile(FileUploadStatus.COMPLETED);
        entityManager.flush();

        assertThat(fileRepository.existsAnyUsageByFileId(linkedFile.getId())).isTrue();
        assertThat(fileRepository.existsAnyUsageByFileId(unlinkedFile.getId())).isFalse();
    }

    private void assertUnlinkedAfterFlush(FileMetadata file) {
        Long fileId = file.getId();
        entityManager.flush();
        entityManager.clear();

        assertThat(fileRepository.existsAnyUsageByFileId(fileId)).isFalse();
        assertThat(entityManager.find(FileMetadata.class, fileId).getStatus()).isEqualTo(FileUploadStatus.COMPLETED);
    }

    private FileMetadata saveFile(FileUploadStatus status) {
        User owner = User.create("파일 소유자", null);
        entityManager.persist(owner);
        FileMetadata file = FileMetadata.create(owner, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        applyStatus(file, status);
        entityManager.persist(file);
        return file;
    }

    private void applyStatus(FileMetadata file, FileUploadStatus status) {
        if (status == FileUploadStatus.PENDING) {
            return;
        }
        file.completeUpload();
        if (status == FileUploadStatus.DELETE_PENDING) {
            file.markForDeletion();
        }
    }

    private Study saveStudy() {
        Study study = Study.create("첨부 연결 조회 스터디", "설명", false);
        entityManager.persist(study);
        return study;
    }

    private Assignment saveAssignment(Study study) {
        Assignment assignment = Assignment.create(study, "과제 제목", "과제 내용", "", SubmissionTarget.MEMBERS_ONLY,
                SubmissionVisibility.LEADER_ONLY, NOW.plusDays(1), NOW);
        entityManager.persist(assignment);
        return assignment;
    }
}
