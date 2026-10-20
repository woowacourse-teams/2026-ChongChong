package withoutc.chongchong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeAttachment;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Transactional
class NoticeAttachmentPersistenceTest extends PostgresContainerTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("공지를 저장하면 첨부 관계와 감사 시각도 함께 저장된다")
    void persistAttachmentsWithNotice() {
        Notice notice = saveNotice();
        FileMetadata first = saveFile();
        FileMetadata second = saveFile();
        notice.addAttachments(List.of(second, first));

        noticeRepository.saveAndFlush(notice);
        Long noticeId = notice.getId();
        entityManager.clear();

        Notice found = noticeRepository.findById(noticeId).orElseThrow();
        assertThat(found.getAttachments())
                .extracting(attachment -> attachment.getFile().getId(), NoticeAttachment::getSortOrder)
                .containsExactly(tuple(second.getId(), 0), tuple(first.getId(), 1));
        assertThat(found.getAttachments()).allSatisfy(attachment -> {
            assertThat(attachment.getId()).isNotNull();
            assertThat(attachment.getNotice().getId()).isEqualTo(noticeId);
            assertThat(attachment.getCreatedAt()).isNotNull();
            assertThat(attachment.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("첨부 행의 저장 순서와 관계없이 표시 순서대로 조회한다")
    void loadAttachmentsInSortOrder() {
        Notice notice = saveNotice();
        FileMetadata first = saveFile();
        FileMetadata second = saveFile();
        entityManager.persist(NoticeAttachment.create(notice, first, 1));
        entityManager.persist(NoticeAttachment.create(notice, second, 0));
        entityManager.flush();
        Long noticeId = notice.getId();
        entityManager.clear();

        Notice found = noticeRepository.findById(noticeId).orElseThrow();

        assertThat(found.getAttachments())
                .extracting(attachment -> attachment.getFile().getId(), NoticeAttachment::getSortOrder)
                .containsExactly(tuple(second.getId(), 0), tuple(first.getId(), 1));
    }

    @Test
    @DisplayName("같은 파일을 서로 다른 공지에 중복 연결할 수 없다")
    void rejectDuplicateFileAcrossNotices() {
        Notice first = saveNotice();
        Notice second = saveNotice();
        FileMetadata file = saveFile();
        insertAttachment(first.getId(), file.getId(), 0);

        assertThatThrownBy(() -> insertAttachment(second.getId(), file.getId(), 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("한 공지 안에서 첨부 표시 순서를 중복 사용할 수 없다")
    void rejectDuplicateOrderWithinNotice() {
        Notice notice = saveNotice();
        FileMetadata first = saveFile();
        FileMetadata second = saveFile();
        insertAttachment(notice.getId(), first.getId(), 0);

        assertThatThrownBy(() -> insertAttachment(notice.getId(), second.getId(), 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB에 직접 저장해도 음수인 첨부 표시 순서는 거부된다")
    void rejectNegativeOrderInDatabase() {
        Notice notice = saveNotice();
        FileMetadata file = saveFile();

        assertThatThrownBy(() -> insertAttachment(notice.getId(), file.getId(), -1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 공지에 첨부를 연결할 수 없다")
    void rejectMissingNotice() {
        FileMetadata file = saveFile();

        assertThatThrownBy(() -> insertAttachment(Long.MAX_VALUE, file.getId(), 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 파일을 공지에 연결할 수 없다")
    void rejectMissingFile() {
        Notice notice = saveNotice();

        assertThatThrownBy(() -> insertAttachment(notice.getId(), Long.MAX_VALUE, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("공지를 SQL로 삭제해도 첨부 관계는 정리되고 파일 메타데이터는 남는다")
    void deleteAttachmentsWithNoticeThroughDatabase() {
        Notice notice = saveNotice();
        FileMetadata file = saveFile();
        insertAttachment(notice.getId(), file.getId(), 0);

        jdbcTemplate.update("DELETE FROM notices WHERE id = ?", notice.getId());

        assertThat(countAttachments(notice.getId())).isZero();
        assertThat(fileRepository.existsById(file.getId())).isTrue();
    }

    @Test
    @DisplayName("JPA로 공지를 삭제해도 첨부 관계만 정리되고 파일 메타데이터는 남는다")
    void deleteAttachmentsWithNoticeThroughJpa() {
        Notice notice = saveNotice();
        FileMetadata file = saveFile();
        notice.addAttachments(List.of(file));
        noticeRepository.saveAndFlush(notice);

        noticeRepository.delete(notice);
        noticeRepository.flush();

        assertThat(countAttachments(notice.getId())).isZero();
        assertThat(fileRepository.existsById(file.getId())).isTrue();
    }

    @Test
    @DisplayName("공지의 첨부 목록에서 제거하면 첨부 행만 삭제된다")
    void deleteOrphanedAttachment() {
        Notice notice = saveNotice();
        FileMetadata file = saveFile();
        notice.addAttachments(List.of(file));
        noticeRepository.saveAndFlush(notice);

        notice.getAttachments().clear();
        noticeRepository.flush();

        assertThat(countAttachments(notice.getId())).isZero();
        assertThat(fileRepository.existsById(file.getId())).isTrue();
    }

    @Test
    @DisplayName("첨부에서 참조하는 파일 메타데이터를 먼저 삭제할 수 없다")
    void rejectDeletingReferencedFile() {
        Notice notice = saveNotice();
        FileMetadata file = saveFile();
        insertAttachment(notice.getId(), file.getId(), 0);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM file_metadata WHERE id = ?", file.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Notice saveNotice() {
        Study study = studyRepository.save(Study.create("첨부 테스트 스터디", "설명"));
        return noticeRepository.saveAndFlush(Notice.create(study, "공지 제목", "공지 내용"));
    }

    private FileMetadata saveFile() {
        User owner = userRepository.save(User.create("파일 소유자", null));
        return fileRepository.saveAndFlush(FileMetadata.create(
                owner, "image.png", "uploads/" + UUID.randomUUID(), "image/png", 1024
        ));
    }

    private void insertAttachment(Long noticeId, Long fileId, int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO notice_attachments (notice_id, file_id, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, noticeId, fileId, sortOrder);
    }

    private Long countAttachments(Long noticeId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notice_attachments WHERE notice_id = ?", Long.class, noticeId
        );
    }
}
