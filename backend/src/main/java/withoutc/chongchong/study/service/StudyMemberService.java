package withoutc.chongchong.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.dto.StudyInviteTokenRequest;
import withoutc.chongchong.study.dto.StudyMemberJoinResponse;
import withoutc.chongchong.study.dto.StudyMembersResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyMemberService {

    private static final int MAX_STUDY_MEMBER_COUNT = 30;

    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final StudyMemberRemover studyMemberRemover;

    private final StudyInviteTokenProvider studyInviteTokenProvider;

    @Transactional
    public StudyMemberJoinResponse join(Long userId, StudyInviteTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Long studyId = studyInviteTokenProvider.verifyAndExtractStudyId(request.token());

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        validateJoin(study.getId(), user.getId());

        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.MEMBER);

        return StudyMemberJoinResponse.from(studyMemberRepository.save(studyMember));
    }

    public StudyMembersResponse getAllStudyMembers(Long userId, Long studyId) {
        studyRepository.getByIdOrThrow(studyId);
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        return StudyMembersResponse.from(studyMemberRepository.findAllSummariesByStudyId(studyId));
    }

    @Transactional
    public void expel(Long userId, Long studyId, Long memberId) {
        StudyMember requester = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeader(requester);

        StudyMember target = studyMemberRepository.getByStudyIdAndIdOrThrow(studyId, memberId);
        if (target.isLeader()) {
            throw new StudyMemberException(StudyMemberErrorCode.STUDY_LEADER_CANNOT_BE_REMOVED);
        }

        studyMemberRemover.remove(target);
    }

    @Transactional
    public void leave(Long userId, Long studyId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        if (member.isLeader()) {
            throw new StudyMemberException(StudyMemberErrorCode.STUDY_LEADER_CANNOT_LEAVE);
        }

        studyMemberRemover.remove(member);
    }

    private void validateJoin(Long studyId, Long userId) {
        if (studyMemberRepository.findByStudyIdAndUserId(studyId, userId).isPresent()) {
            throw new StudyMemberException(StudyMemberErrorCode.ALREADY_JOINED_STUDY);
        }

        if (studyMemberRepository.countByStudyId(studyId) >= MAX_STUDY_MEMBER_COUNT) {
            throw new StudyMemberException(StudyMemberErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }
    }

    private void validateLeader(StudyMember member) {
        if (!member.isLeader()) {
            throw new StudyMemberException(StudyMemberErrorCode.NOT_STUDY_LEADER);
        }
    }
}
