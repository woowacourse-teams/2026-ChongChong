package withoutc.chongchong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("스터디 공지를 최신순으로 조회하고 cursor보다 작은 id부터 다음 페이지를 조회한다")
    void findByCursorTest() {
        StudyFixture firstStudy = createStudyFixture("첫 번째 스터디", "첫 번째 리더");
        StudyFixture secondStudy = createStudyFixture("두 번째 스터디", "두 번째 리더");
        List<Notice> notices = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            notices.add(noticeRepository.save(
                    Notice.create(firstStudy.study(), firstStudy.leader(), "공지 " + index, "공지 내용")
            ));
        }
        noticeRepository.save(Notice.create(secondStudy.study(), secondStudy.leader(), "다른 공지", "공지 내용"));
        noticeRepository.flush();

        List<Notice> firstPage = noticeRepository.findByCursor(
                firstStudy.study().getId(),
                null,
                PageRequest.of(0, 3)
        );
        Long cursor = firstPage.getLast().getId();
        List<Notice> secondPage = noticeRepository.findByCursor(
                firstStudy.study().getId(),
                cursor,
                PageRequest.of(0, 3)
        );

        assertThat(firstPage)
                .extracting(Notice::getId)
                .containsExactly(notices.get(4).getId(), notices.get(3).getId(), notices.get(2).getId());
        assertThat(secondPage)
                .extracting(Notice::getId)
                .containsExactly(notices.get(1).getId(), notices.getFirst().getId());
    }

    @Test
    @DisplayName("공지 조회에 성공하면 해당 공지를 반환한다")
    void getByIdOrThrowTest() {
        StudyFixture fixture = createStudyFixture("스터디", "리더");
        Notice notice = noticeRepository.save(
                Notice.create(fixture.study(), fixture.leader(), "공지", "공지 내용")
        );

        Notice found = noticeRepository.getByIdOrThrow(notice.getId());

        assertThat(found.getId()).isEqualTo(notice.getId());
    }

    @Test
    @DisplayName("공지가 존재하지 않으면 공지 없음 예외를 던진다")
    void getByIdOrThrowNotFoundTest() {
        assertThatThrownBy(() -> noticeRepository.getByIdOrThrow(Long.MAX_VALUE))
                .isInstanceOfSatisfying(NoticeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND)
                );
    }

    private StudyFixture createStudyFixture(String studyName, String userName) {
        User user = userRepository.save(User.create(userName, null));
        Study study = studyRepository.save(Study.create(studyName, "설명"));
        StudyMember leader = studyMemberRepository.save(
                StudyMember.create(study, user, userName, null, StudyMemberRole.LEADER)
        );
        return new StudyFixture(study, leader);
    }

    private record StudyFixture(Study study, StudyMember leader) {
    }
}
