package withoutc.chongchong.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notice.entity.Notice;
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
class NoticeRecipientRepositoryTest {

    @Autowired
    private NoticeRecipientRepository noticeRecipientRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("공지 수신자는 User id가 아니라 StudyMember id와 공지 id로 조회한다")
    void findByNoticeIdAndMemberIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember writer = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember recipient = createMemberWithIdDifferentFromUserId(study);
        Notice notice = Notice.create(study, writer, "공지", "공지 내용");
        notice.addRecipients(List.of(recipient));
        noticeRepository.saveAndFlush(notice);
        Notice otherNotice = noticeRepository.saveAndFlush(
                Notice.create(study, writer, "다른 공지", "다른 공지 내용")
        );

        assertThat(recipient.getId()).isNotEqualTo(recipient.getUser().getId());
        assertThat(noticeRecipientRepository.findByNoticeIdAndMemberId(notice.getId(), recipient.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getNotice().getId()).isEqualTo(notice.getId());
                    assertThat(found.getMember().getId()).isEqualTo(recipient.getId());
                });
        assertThat(noticeRecipientRepository.findByNoticeIdAndMemberId(
                notice.getId(), recipient.getUser().getId()
        )).isEmpty();
        assertThat(noticeRecipientRepository.findByNoticeIdAndMemberId(
                otherNotice.getId(), recipient.getId()
        )).isEmpty();
    }

    private StudyMember createMemberWithIdDifferentFromUserId(Study study) {
        StudyMember candidate = createMember(study, "스터디원", StudyMemberRole.MEMBER);
        if (!candidate.getId().equals(candidate.getUser().getId())) {
            return candidate;
        }

        userRepository.save(User.create("ID 간격 생성용 사용자", null));
        return createMember(study, "다른 스터디원", StudyMemberRole.MEMBER);
    }

    private StudyMember createMember(Study study, String name, StudyMemberRole role) {
        User user = userRepository.save(User.create(name, null));
        return studyMemberRepository.save(StudyMember.create(study, user, name, null, role));
    }
}
