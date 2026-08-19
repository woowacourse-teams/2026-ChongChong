package withoutc.chongchong.study.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.dto.MyStudyListResponse;
import withoutc.chongchong.study.dto.MyStudyListResponse.MyStudyResponse;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyCreateResponse;
import withoutc.chongchong.study.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private static final int MAX_JOINED_STUDY_COUNT = 50;

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;

    private final StudyInviteLinkGenerator studyInviteLinkGenerator;

    @Transactional
    public StudyCreateResponse create(Long userId, StudyCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        validateStudyCountLimit(userId);

        Study study = studyRepository.save(request.toStudy());

        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.LEADER);

        studyMemberRepository.save(studyMember);

        return StudyCreateResponse.from(study);
    }

    private void validateStudyCountLimit(Long userId) {
        if (studyMemberRepository.countByUserId(userId) >= MAX_JOINED_STUDY_COUNT) {
            throw new StudyMemberException(StudyMemberErrorCode.JOINED_STUDY_LIMIT_EXCEEDED);
        }
    }

    public MyStudyListResponse getMyStudies(Long userId) {
        List<StudyMember> members = studyMemberRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        List<MyStudyResponse> responses = new ArrayList<>();
        for (StudyMember member : members) {
            Study study = member.getStudy();

            int memberCount = studyMemberRepository.countByStudyId(study.getId());
            int noticeCount = unReadNoticeCount(member.getRole());
            int assignmentCount = unFinishedAssignmentCount(member.getRole());

            responses.add(MyStudyResponse.from(study, member.getRole(), memberCount, noticeCount, assignmentCount));
        }

        return new MyStudyListResponse(responses.size(), responses);
    }

    // TODO: 현재 Mock 데이터. NoticeRecipient 구현되면 변경
    private int unReadNoticeCount(StudyMemberRole role) {
        if (role == StudyMemberRole.LEADER) {
            return 5;
        }
        return 1;
    }

    // TODO: 현재 Mock 데이터. Submission 구현되면 변경
    private int unFinishedAssignmentCount(StudyMemberRole role) {
        if (role == StudyMemberRole.LEADER) {
            return 5;
        }
        return 1;
    }

    public StudyInviteLinkResponse getInviteLink(Long userId, Long studyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        studyMemberRepository.findByStudyIdAndUserId(study.getId(), user.getId())
                .orElseThrow(() -> new StudyMemberException(StudyMemberErrorCode.NOT_STUDY_MEMBER));

        return new StudyInviteLinkResponse(studyInviteLinkGenerator.generate(studyId));
    }
}
