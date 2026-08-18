package withoutc.chongchong.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyCreateResponse;
import withoutc.chongchong.study.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyInviteLinkGenerator studyInviteLinkGenerator;

    @Transactional
    public StudyCreateResponse create(Long userId, StudyCreateRequest request) {
        // TODO: 나중에 UserException으로 변경
        User user = userRepository.findById(userId)
                .orElseThrow();

        Study study = studyRepository.save(request.toStudy());

        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.LEADER);

        // TODO: StudyMemberService에서 멤버 생성 시 사용자별 가입 스터디 50개 제한 검증
        // TODO: 스터디 생성 시 리더 등록과, 스터디 참가 모두 StudyMemberService 사용
        studyMemberRepository.save(studyMember);

        return StudyCreateResponse.from(study);
    }

    // TODO: 인증, 인가 구현 후 사용자가 해당 스터디에 속해 있는지 확인
    public StudyInviteLinkResponse getInviteLink(Long studyId) {
        studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        return new StudyInviteLinkResponse(studyInviteLinkGenerator.generate(studyId));
    }
}
