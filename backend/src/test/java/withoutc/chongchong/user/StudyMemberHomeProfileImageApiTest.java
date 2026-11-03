package withoutc.chongchong.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeRecipient;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberProfileImageProjection;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@Import(TestS3PresignerConfiguration.class)
class StudyMemberHomeProfileImageApiTest extends PostgresContainerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository memberRepository;
    @Autowired
    private NoticeRepository noticeRepository;
    @Autowired
    private NoticeRecipientRepository recipientRepository;
    @Autowired
    private FileMetaDataRepository fileRepository;
    @Autowired
    private TestAuthRequest auth;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockitoSpyBean
    private FileUrlGenerator urlGenerator;

    @LocalServerPort
    private int port;

    private User leader;
    private User target;
    private Study firstStudy;
    private Study secondStudy;
    private StudyMember firstMember;
    private StudyMember secondMember;
    private StudyMember otherMember;
    private Notice firstNotice;
    private Notice secondNotice;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leader = userRepository.saveAndFlush(User.create("리더", null));
        target = userRepository.saveAndFlush(User.create("홈 이름", "https://example.com/home.png"));
        User other = userRepository.saveAndFlush(User.create("다른 사용자", "https://example.com/other.png"));
        firstStudy = studyRepository.saveAndFlush(Study.create("첫 스터디", "설명", false));
        secondStudy = studyRepository.saveAndFlush(Study.create("둘째 스터디", "설명", false));
        saveMember(firstStudy, leader, "리더", null, StudyMemberRole.LEADER);
        saveMember(secondStudy, leader, "리더", null, StudyMemberRole.LEADER);
        firstMember = saveMember(firstStudy, target, "첫 스터디 이름", "https://example.com/stale-first.png",
                StudyMemberRole.MEMBER);
        secondMember = saveMember(secondStudy, target, "둘째 스터디 이름", "https://example.com/stale-second.png",
                StudyMemberRole.MEMBER);
        otherMember = saveMember(firstStudy, other, "다른 멤버", "https://example.com/stale-other.png",
                StudyMemberRole.MEMBER);
        firstNotice = noticeRepository.saveAndFlush(Notice.create(firstStudy, "첫 공지", "내용"));
        secondNotice = noticeRepository.saveAndFlush(Notice.create(secondStudy, "둘째 공지", "내용"));
        NoticeRecipient read = NoticeRecipient.create(firstMember, firstNotice);
        read.markAsRead(LocalDateTime.of(2026, 11, 3, 10, 0));
        recipientRepository.saveAndFlush(read);
        recipientRepository.saveAndFlush(NoticeRecipient.create(otherMember, firstNotice));
        recipientRepository.saveAndFlush(NoticeRecipient.create(secondMember, secondNotice));
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("스터디에 복사된 URL과 달라도 멤버 목록·읽음/미읽음 현황은 현재 홈 외부 URL을 사용한다")
    void usesHomeExternalUrlInsteadOfCopiedMemberUrl() {
        assertTargetImages("https://example.com/home.png", false);
        assertThat(image(members(firstStudy.getId()), "members", otherMember.getId()))
                .isEqualTo("https://example.com/other.png");
        Response statuses = statuses(firstStudy.getId(), firstNotice.getId());
        assertThat(image(statuses, "unreadMembers", otherMember.getId())).isEqualTo("https://example.com/other.png");
        assertThat(statuses.jsonPath().getInt("memberCount")).isEqualTo(2);
        assertThat(statuses.jsonPath().getInt("readCount")).isEqualTo(1);
        assertThat(statuses.jsonPath().getInt("unreadCount")).isEqualTo(1);
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("홈 업로드 이미지를 바꾸면 두 스터디의 멤버 목록과 공지 읽음 현황에 새 파일이 반영된다")
    void reflectsReplacementAcrossStudiesAndReadStatuses() {
        FileMetadata first = saveImage();
        changeHomeImage(first);
        assertTargetImages(first.getObjectKey(), true);

        FileMetadata replacement = saveImage();
        changeHomeImage(replacement);
        assertTargetImages(replacement.getObjectKey(), true);
        assertThat(image(members(firstStudy.getId()), "members", otherMember.getId()))
                .isEqualTo("https://example.com/other.png");
    }

    @Test
    @DisplayName("홈 초기화는 모든 대상 목록에 null을 반영하고 멤버에 저장된 예전 URL을 되살리지 않는다")
    void reflectsResetWithoutRestoringCopiedImage() {
        FileMetadata file = saveImage();
        changeHomeImage(file);

        Response reset = auth.givenAuthenticatedUser(target.getId()).port(port).delete("/users/me/profile-image");

        assertThat(reset.statusCode()).isEqualTo(204);
        assertTargetImages(null, false);
        assertThat(jdbc.queryForObject("SELECT profile_image_url FROM study_members WHERE id = ?", String.class,
                firstMember.getId())).isEqualTo("https://example.com/stale-first.png");
        assertThat(jdbc.queryForObject("SELECT profile_image_url FROM study_members WHERE id = ?", String.class,
                secondMember.getId())).isEqualTo("https://example.com/stale-second.png");
        assertThat(fileRepository.existsAnyUsageByFileId(file.getId())).isFalse();
    }

    @Test
    @DisplayName("인증·스터디 접근·리더 권한·공지 소속 확인에 실패하면 이미지 URL을 생성하지 않는다")
    void checksAccessBeforeResolvingImageUrls() {
        User outsider = userRepository.saveAndFlush(User.create("외부 사용자", null));
        assertThat(given().port(port).get("/api/studies/{id}/members", firstStudy.getId()).statusCode()).isEqualTo(401);
        assertThat(auth.givenAuthenticatedUser(outsider.getId()).port(port)
                .get("/studies/{id}/members", firstStudy.getId()).statusCode()).isEqualTo(403);
        assertThat(auth.givenAuthenticatedUser(target.getId()).port(port)
                .get("/studies/{id}/notices/{noticeId}/status", firstStudy.getId(), firstNotice.getId()).statusCode())
                .isEqualTo(403);
        assertThat(auth.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{id}/notices/{noticeId}/status", firstStudy.getId(), secondNotice.getId()).statusCode())
                .isEqualTo(404);
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("확인 대상자가 없는 공지는 이미지 조회 없이 빈 읽음 현황을 반환한다")
    void readsEmptyRecipientList() {
        Notice notice = noticeRepository.saveAndFlush(Notice.create(firstStudy, "빈 공지", "내용"));

        Response response = statuses(firstStudy.getId(), notice.getId());

        assertThat(response.jsonPath().getInt("memberCount")).isZero();
        assertThat(response.jsonPath().getList("readMembers")).isEmpty();
        assertThat(response.jsonPath().getList("unreadMembers")).isEmpty();
        verifyNoInteractions(urlGenerator);
    }

    @Test
    @DisplayName("이미지가 없는 멤버도 포함해 50명의 이미지 원본을 엔티티 로딩 없이 한 번에 조회한다")
    void loadsFiftyImageSourcesInOneQuery() {
        for (int index = 0; index < 47; index++) {
            User user = userRepository.saveAndFlush(User.create("멤버", null));
            saveMember(firstStudy, user, "멤버", null, StudyMemberRole.MEMBER);
        }
        List<Long> memberIds = jdbc.queryForList("SELECT id FROM study_members WHERE study_id = ?", Long.class,
                firstStudy.getId());
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<StudyMemberProfileImageProjection> images = memberRepository.findProfileImagesByMemberIds(memberIds);

        assertThat(images).hasSize(50);
        assertThat(images).extracting(StudyMemberProfileImageProjection::memberId)
                .containsExactlyInAnyOrderElementsOf(memberIds);
        long imageQueries = Arrays.stream(statistics.getQueries())
                .filter(query -> query.contains("StudyMemberProfileImageProjection"))
                .mapToLong(query -> statistics.getQueryStatistics(query).getExecutionCount())
                .sum();
        assertThat(imageQueries).isEqualTo(1);
        assertThat(statistics.getEntityLoadCount()).isZero();
    }

    private void assertTargetImages(String expected, boolean signed) {
        assertImage(image(members(firstStudy.getId()), "members", firstMember.getId()), expected, signed);
        assertImage(image(members(secondStudy.getId()), "members", secondMember.getId()), expected, signed);
        assertImage(image(statuses(firstStudy.getId(), firstNotice.getId()), "readMembers", firstMember.getId()),
                expected, signed);
        assertImage(image(statuses(secondStudy.getId(), secondNotice.getId()), "unreadMembers", secondMember.getId()),
                expected, signed);
    }

    private void assertImage(String actual, String expected, boolean signed) {
        if (signed) {
            assertThat(actual).startsWith("https://").contains(expected, "X-Amz-Signature=");
            return;
        }
        assertThat(actual).isEqualTo(expected);
    }

    private String image(Response response, String listPath, Long memberId) {
        List<Map<String, Object>> members = response.jsonPath().getList(listPath);
        return (String) members.stream()
                .filter(member -> ((Number) member.get("id")).longValue() == memberId)
                .findFirst().orElseThrow().get("profileImage");
    }

    private Response members(Long studyId) {
        Response response = auth.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{id}/members", studyId);
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private Response statuses(Long studyId, Long noticeId) {
        Response response = auth.givenAuthenticatedUser(leader.getId()).port(port)
                .get("/studies/{id}/notices/{noticeId}/status", studyId, noticeId);
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private FileMetadata saveImage() {
        FileMetadata file = FileMetadata.create(target, "avatar.png", "uploads/" + UUID.randomUUID(), "image/png", 1024);
        file.completeUpload();
        return fileRepository.saveAndFlush(file);
    }

    private void changeHomeImage(FileMetadata file) {
        Response response = auth.givenAuthenticatedUser(target.getId()).port(port).contentType(ContentType.JSON)
                .body(Map.of("fileId", file.getId())).patch("/users/me/profile-image");
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private StudyMember saveMember(Study study, User user, String name, String imageUrl, StudyMemberRole role) {
        return memberRepository.saveAndFlush(StudyMember.create(study, user, name, imageUrl, role));
    }
}
