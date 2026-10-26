package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.study.controller.dto.StudyListResponse;
import withoutc.chongchong.study.controller.dto.StudySummaryResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.service.StudyService;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestS3PresignerConfiguration.class)
@Transactional
class StudyPublicDiscoveryTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 26, 12, 0);

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("공개 스터디를 생성 시각과 ID 역순으로 중복 없이 탐색하고 식별자와 요약 정보를 반환한다")
    void discoverPublicStudiesByCreatedAtAndId() {
        User user = userRepository.saveAndFlush(User.create("사용자", null));
        Study sameTimeLowerId = saveStudy("같은 시각 작은 ID", true, CREATED_AT);
        Study oldest = saveStudy("가장 오래된 스터디", true, CREATED_AT.minusHours(2));
        saveStudy("최신 비공개 스터디", false, CREATED_AT.plusHours(1));
        Study deleted = saveStudy("삭제할 스터디", true, CREATED_AT);
        studyRepository.delete(deleted);
        studyRepository.flush();
        Study sameTimeHigherId = saveStudy("같은 시각 큰 ID", true, CREATED_AT);
        Study olderHigherId = saveStudy("이전 시각 큰 ID", true, CREATED_AT.minusHours(1));
        saveStudy("이전 비공개 스터디", false, CREATED_AT.minusMinutes(90));

        FileMetadata image = FileMetadata.create(user, "cover.png", "study-images/cover.png", "image/png", 1024);
        image.completeUpload();
        fileRepository.saveAndFlush(image);
        sameTimeHigherId.changeImage(image);
        studyMemberRepository.saveAndFlush(
                StudyMember.create(sameTimeHigherId, user, user.getName(), null, StudyMemberRole.LEADER));
        entityManager.flush();
        entityManager.clear();

        StudyListResponse firstPage = studyService.getPublicStudies(user.getId(), null, null, 1);

        assertPage(firstPage, sameTimeHigherId.getId(), true);
        StudySummaryResponse summary = firstPage.studies().getFirst();
        assertThat(summary.studyName()).isEqualTo("같은 시각 큰 ID");
        assertThat(summary.description()).isEqualTo("설명");
        assertThat(summary.memberCount()).isEqualTo(1);
        assertThat(summary.imageUrl()).contains("study-images/cover.png", "X-Amz-Signature=");

        StudyListResponse secondPage = studyService.getPublicStudies(user.getId(), null, firstPage.nextCursor(), 1);
        assertPage(secondPage, sameTimeLowerId.getId(), true);
        assertThat(secondPage.studies().getFirst().imageUrl()).isNull();

        StudyListResponse thirdPage = studyService.getPublicStudies(user.getId(), null, secondPage.nextCursor(), 1);
        assertPage(thirdPage, olderHigherId.getId(), true);

        StudyListResponse lastPage = studyService.getPublicStudies(user.getId(), null, thirdPage.nextCursor(), 1);
        assertPage(lastPage, oldest.getId(), false);
    }

    @Test
    @DisplayName("49명인 공개 스터디는 노출하고 50명 이상은 페이징 전에 제외한다")
    void excludeFullStudiesBeforeCursorPaging() {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study full = saveStudy("정원이 찬 스터디", true, CREATED_AT.plusHours(1));
        Study available = saveStudy("49명 스터디", true, CREATED_AT);
        Study overfilled = saveStudy("정원 초과 스터디", true, CREATED_AT.minusMinutes(30));
        Study older = saveStudy("이전 스터디", true, CREATED_AT.minusHours(1));
        addStudyMembers(full, 50);
        addStudyMembers(available, 49);
        addStudyMembers(overfilled, 51);
        addStudyMembers(older, 1);
        entityManager.flush();
        entityManager.clear();

        StudyListResponse firstPage = studyService.getPublicStudies(viewer.getId(), null, null, 1);

        assertPage(firstPage, available.getId(), true);
        assertThat(firstPage.studies().getFirst().memberCount()).isEqualTo(49);

        StudyListResponse lastPage = studyService.getPublicStudies(viewer.getId(), null, firstPage.nextCursor(), 1);

        assertPage(lastPage, older.getId(), false);
    }

    @Test
    @DisplayName("공개 스터디가 없으면 빈 목록과 마지막 페이지 정보를 반환한다")
    void returnEmptyPageWhenOnlyPrivateStudiesExist() {
        User user = userRepository.saveAndFlush(User.create("사용자", null));
        saveStudy("비공개 스터디", false, CREATED_AT);
        entityManager.clear();

        StudyListResponse response = studyService.getPublicStudies(user.getId(), null, null, 10);

        assertThat(response.studies()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("커서에 해당하는 스터디가 없으면 스터디 없음 예외를 반환한다")
    void rejectMissingCursorStudy() {
        User user = userRepository.saveAndFlush(User.create("사용자", null));

        assertThatThrownBy(() -> studyService.getPublicStudies(user.getId(), null, Long.MAX_VALUE, 10))
                .isInstanceOfSatisfying(StudyException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StudyErrorCode.STUDY_NOT_FOUND));
    }

    @Test
    @DisplayName("이름이 포함된 공개 스터디만 정원과 생성 시각 및 ID 기준으로 검색한다")
    void searchAvailablePublicStudiesByName() {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study sameTimeLowerId = saveStudy("자바 기초", true, CREATED_AT);
        Study older = saveStudy("심화 자바", true, CREATED_AT.minusHours(1));
        saveStudy("자바 비공개", false, CREATED_AT.plusHours(2));
        Study full = saveStudy("자바 정원", true, CREATED_AT.plusHours(1));
        addStudyMembers(full, 50);
        Study sameTimeHigherId = saveStudy("자바 실전", true, CREATED_AT);
        saveStudy("파이썬", true, CREATED_AT.plusHours(3));
        entityManager.flush();
        entityManager.clear();

        StudyListResponse firstPage = studyService.getPublicStudies(viewer.getId(), "자바", null, 1);
        assertPage(firstPage, sameTimeHigherId.getId(), true);

        StudyListResponse secondPage = studyService.getPublicStudies(viewer.getId(), "자바", firstPage.nextCursor(), 1);
        assertPage(secondPage, sameTimeLowerId.getId(), true);

        StudyListResponse lastPage = studyService.getPublicStudies(viewer.getId(), "자바", secondPage.nextCursor(), 1);
        assertPage(lastPage, older.getId(), false);
    }

    @Test
    @DisplayName("검색어의 와일드카드는 문자 그대로 취급하고 영문 대소문자는 구분하지 않는다")
    void searchTreatsWildcardsLiterally() {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study percent = saveStudy("Java% 모임", true, CREATED_AT);
        Study underscore = saveStudy("Java_ 모임", true, CREATED_AT.minusHours(1));
        Study general = saveStudy("Java 일반", true, CREATED_AT.plusHours(1));
        entityManager.clear();

        assertThat(studyService.getPublicStudies(viewer.getId(), "%", null, 10).studies())
                .extracting(StudySummaryResponse::studyId).containsExactly(percent.getId());
        assertThat(studyService.getPublicStudies(viewer.getId(), "_", null, 10).studies())
                .extracting(StudySummaryResponse::studyId).containsExactly(underscore.getId());
        assertThat(studyService.getPublicStudies(viewer.getId(), "java", null, 10).studies())
                .extracting(StudySummaryResponse::studyId)
                .containsExactly(general.getId(), percent.getId(), underscore.getId());
    }

    private void addStudyMembers(Study study, int count) {
        for (int i = 0; i < count; i++) {
            User member = userRepository.save(User.create("멤버" + i, null));
            StudyMemberRole role = i == 0 ? StudyMemberRole.LEADER : StudyMemberRole.MEMBER;
            studyMemberRepository.save(StudyMember.create(study, member, member.getName(), null, role));
        }
        studyMemberRepository.flush();
    }

    private Study saveStudy(String name, boolean isPublic, LocalDateTime createdAt) {
        Study study = studyRepository.saveAndFlush(Study.create(name, "설명", isPublic));
        jdbcTemplate.update("UPDATE studies SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt), study.getId());
        return study;
    }

    private void assertPage(StudyListResponse page, Long studyId, boolean hasNext) {
        assertThat(page.studies()).extracting(StudySummaryResponse::studyId).containsExactly(studyId);
        assertThat(page.hasNext()).isEqualTo(hasNext);
        assertThat(page.nextCursor()).isEqualTo(hasNext ? studyId : null);
    }
}
