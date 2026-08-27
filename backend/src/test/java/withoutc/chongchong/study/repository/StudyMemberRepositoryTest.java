package withoutc.chongchong.study.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.projection.StudyMemberSummaryProjection;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class StudyMemberRepositoryTest {

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("스터디 id와 사용자 id가 모두 일치하는 스터디원을 조회한다")
    void findByStudyIdAndUserIdTest() {
        User user = userRepository.save(User.create("사용자", null));
        User otherUser = userRepository.save(User.create("다른 사용자", null));
        Study firstStudy = studyRepository.save(Study.create("첫 번째 스터디", "설명"));
        Study secondStudy = studyRepository.save(Study.create("두 번째 스터디", "설명"));
        StudyMember firstMember = saveMember(firstStudy, user, "첫 번째 멤버");
        StudyMember secondMember = saveMember(secondStudy, user, "두 번째 멤버");
        saveMember(firstStudy, otherUser, "다른 멤버");

        assertThat(studyMemberRepository.findByStudyIdAndUserId(firstStudy.getId(), user.getId()))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(firstMember.getId()));
        assertThat(studyMemberRepository.findByStudyIdAndUserId(secondStudy.getId(), user.getId()))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(secondMember.getId()));
        assertThat(studyMemberRepository.findByStudyIdAndUserId(secondStudy.getId(), otherUser.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("스터디에 속한 스터디원만 모두 조회한다")
    void findAllByStudyIdTest() {
        Study firstStudy = studyRepository.save(Study.create("첫 번째 스터디", "설명"));
        Study secondStudy = studyRepository.save(Study.create("두 번째 스터디", "설명"));
        StudyMember firstMember = createMember(firstStudy, "첫 번째 멤버");
        StudyMember secondMember = createMember(firstStudy, "두 번째 멤버");
        createMember(secondStudy, "다른 스터디 멤버");

        assertThat(studyMemberRepository.findAllByStudyId(firstStudy.getId()))
                .extracting(StudyMember::getId)
                .containsExactlyInAnyOrder(firstMember.getId(), secondMember.getId());
    }

    @Test
    @DisplayName("스터디원 조회에 성공하면 해당 스터디원을 반환한다")
    void getByStudyIdAndUserIdOrThrowTest() {
        User user = userRepository.save(User.create("사용자", null));
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember member = saveMember(study, user, "스터디원");

        StudyMember found = studyMemberRepository.getByStudyIdAndUserIdOrThrow(study.getId(), user.getId());

        assertThat(found.getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("스터디원이 존재하지 않으면 스터디 접근 거부 예외를 던진다")
    void getByStudyIdAndUserIdOrThrowNotFoundTest() {
        assertThatThrownBy(() -> studyMemberRepository.getByStudyIdAndUserIdOrThrow(
                Long.MAX_VALUE, Long.MAX_VALUE
        ))
                .isInstanceOfSatisfying(StudyMemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StudyMemberErrorCode.STUDY_ACCESS_DENIED)
                );
    }

    @Test
    @DisplayName("스터디 멤버 목록을 리더 우선, 가입 순서대로 조회한다")
    void findAllSummariesByStudyIdTest() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        Study otherStudy = studyRepository.saveAndFlush(Study.create("다른 스터디", "설명"));

        StudyMember firstMember = saveMember(
                study,
                "첫 번째 멤버",
                null,
                StudyMemberRole.MEMBER
        );
        StudyMember secondMember = saveMember(
                study,
                "두 번째 멤버",
                "member-profile-image-url",
                StudyMemberRole.MEMBER
        );
        StudyMember leader = saveMember(
                study,
                "리더",
                "leader-profile-image-url",
                StudyMemberRole.LEADER
        );

        saveMember(
                otherStudy,
                "다른 스터디 멤버",
                null,
                StudyMemberRole.MEMBER
        );

        List<StudyMemberSummaryProjection> result =
                studyMemberRepository.findAllSummariesByStudyId(study.getId());

        assertThat(result)
                .extracting(
                        StudyMemberSummaryProjection::id,
                        StudyMemberSummaryProjection::name,
                        StudyMemberSummaryProjection::profileImageUrl,
                        StudyMemberSummaryProjection::role
                )
                .containsExactly(
                        tuple(
                                leader.getId(),
                                "리더",
                                "leader-profile-image-url",
                                StudyMemberRole.LEADER
                        ),
                        tuple(
                                firstMember.getId(),
                                "첫 번째 멤버",
                                null,
                                StudyMemberRole.MEMBER
                        ),
                        tuple(
                                secondMember.getId(),
                                "두 번째 멤버",
                                "member-profile-image-url",
                                StudyMemberRole.MEMBER
                        )
                );
    }

    private StudyMember createMember(Study study, String name) {
        User user = userRepository.save(User.create(name, null));
        return saveMember(study, user, name);
    }

    private StudyMember saveMember(Study study, User user, String name) {
        return studyMemberRepository.save(
                StudyMember.create(study, user, name, null, StudyMemberRole.MEMBER)
        );
    }

    private StudyMember saveMember(
            Study study,
            String name,
            String profileImageUrl,
            StudyMemberRole role
    ) {
        User user = userRepository.save(User.create(name, profileImageUrl));

        return studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, name, profileImageUrl, role)
        );
    }
}
