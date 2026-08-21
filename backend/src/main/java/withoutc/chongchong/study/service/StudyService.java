package withoutc.chongchong.study.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.dto.LeaderStudyDetailResponse;
import withoutc.chongchong.study.dto.LeaderStudyDetailResponse.LeaderAssignmentSummaryListResponse;
import withoutc.chongchong.study.dto.LeaderStudyDetailResponse.LeaderAssignmentSummaryResponse;
import withoutc.chongchong.study.dto.LeaderStudyDetailResponse.LeaderNoticeSummaryListResponse;
import withoutc.chongchong.study.dto.LeaderStudyDetailResponse.LeaderNoticeSummaryResponse;
import withoutc.chongchong.study.dto.MemberStudyDetailResponse;
import withoutc.chongchong.study.dto.MemberStudyDetailResponse.MemberAssignmentSummaryListResponse;
import withoutc.chongchong.study.dto.MemberStudyDetailResponse.MemberAssignmentSummaryResponse;
import withoutc.chongchong.study.dto.MemberStudyDetailResponse.MemberNoticeSummaryListResponse;
import withoutc.chongchong.study.dto.MemberStudyDetailResponse.MemberNoticeSummaryResponse;
import withoutc.chongchong.study.dto.MyStudyListResponse;
import withoutc.chongchong.study.dto.MyStudyListResponse.MyStudyResponse;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyCreateResponse;
import withoutc.chongchong.study.dto.StudyDetailResponse;
import withoutc.chongchong.study.dto.StudyInfoResponse;
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
    private final NoticeRepository noticeRepository;
    private final AssignmentRepository assignmentRepository;

    private final StudyInviteLinkGenerator studyInviteLinkGenerator;

    @Transactional
    public StudyCreateResponse createStudy(Long userId, StudyCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        validateStudyCountLimit(userId);

        Study study = studyRepository.save(request.toStudy());

        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.LEADER);

        studyMemberRepository.save(studyMember);

        return StudyCreateResponse.from(study);
    }

    @Transactional
    public void deleteStudy(Long userId, Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        if (studyMember.getRole() != StudyMemberRole.LEADER) {
            throw new StudyMemberException(StudyMemberErrorCode.NOT_STUDY_LEADER);
        }

        assignmentRepository.deleteAllByStudyId(studyId);
        noticeRepository.deleteAllByStudyId(studyId);
        studyMemberRepository.deleteAllByStudyId(studyId);
        studyRepository.delete(study);
    }

    private void validateStudyCountLimit(Long userId) {
        if (studyMemberRepository.countByUserId(userId) >= MAX_JOINED_STUDY_COUNT) {
            throw new StudyMemberException(StudyMemberErrorCode.JOINED_STUDY_LIMIT_EXCEEDED);
        }
    }

    public StudyInfoResponse getStudyInfo(Long userId, Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        return new StudyInfoResponse(study.getName(), studyMember.getRole(), studyMember.getName());
    }

    public StudyDetailResponse getStudyDetail(Long userId, Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        if (studyMember.getRole() == StudyMemberRole.LEADER) {
            return makeLeaderStudyDetailResponse(study);
        }
        return makeMemberStudyDetailResponse(study);
    }

    private LeaderStudyDetailResponse makeLeaderStudyDetailResponse(Study study) {
        int memberCount = studyMemberRepository.countByStudyId(study.getId());

        // TODO: NoticeRecipient, Submission 구현되면 한 명이라도 안 읽은 공지, 한 명이라도 제출 안 한 과제로 변경
        List<Notice> notices = noticeRepository.findAllByStudyId(study.getId());
        List<Assignment> assignments = assignmentRepository.findAllByStudyId(study.getId());

        List<LeaderNoticeSummaryResponse> noticeResponses = new ArrayList<>();
        List<LeaderAssignmentSummaryResponse> assignmentResponses = new ArrayList<>();

        for (Notice notice : notices) {
            LeaderNoticeSummaryResponse response = LeaderNoticeSummaryResponse.from(notice, 2);
            noticeResponses.add(response);
        }

        for (Assignment assignment : assignments) {
            LeaderAssignmentSummaryResponse response = LeaderAssignmentSummaryResponse.from(assignment, 2);
            assignmentResponses.add(response);
        }

        return new LeaderStudyDetailResponse(memberCount, LeaderNoticeSummaryListResponse.from(noticeResponses),
                LeaderAssignmentSummaryListResponse.from(assignmentResponses));
    }

    private MemberStudyDetailResponse makeMemberStudyDetailResponse(Study study) {
        // TODO: NoticeRecipient, Submission 구현되면 안 읽은 공지 + 제출 안 한 과제로 변경
        int totalCount = 4;

        // TODO: NoticeRecipient, Submission 구현되면 안 읽은 공지, 제출 안 한 과제로 변경
        List<Notice> notices = noticeRepository.findAllByStudyId(study.getId());
        List<Assignment> assignments = assignmentRepository.findAllByStudyId(study.getId());

        List<MemberNoticeSummaryResponse> noticeResponses = notices.stream()
                .map(MemberNoticeSummaryResponse::from)
                .toList();

        List<MemberAssignmentSummaryResponse> assignmentResponses = assignments.stream()
                .map(MemberAssignmentSummaryResponse::from)
                .toList();

        return new MemberStudyDetailResponse(totalCount, MemberNoticeSummaryListResponse.from(noticeResponses),
                MemberAssignmentSummaryListResponse.from(assignmentResponses));
    }

    public MyStudyListResponse getMyStudies(Long userId) {
        List<StudyMember> studyMembers = studyMemberRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        List<MyStudyResponse> responses = new ArrayList<>();
        for (StudyMember studyMember : studyMembers) {
            Study study = studyMember.getStudy();

            int memberCount = studyMemberRepository.countByStudyId(study.getId());
            int noticeCount = unReadNoticeCount(studyMember.getRole());
            int assignmentCount = unFinishedAssignmentCount(studyMember.getRole());

            responses.add(
                    MyStudyResponse.from(study, studyMember.getRole(), memberCount, noticeCount, assignmentCount));
        }

        return new MyStudyListResponse(responses.size(), responses);
    }

    // TODO: 현재 Mock 데이터. NoticeRecipient 구현되면 리더는 한 명이라도 안 읽은 공지, 멤버는 자신이 안 읽은 공지 개수로 변경
    private int unReadNoticeCount(StudyMemberRole role) {
        if (role == StudyMemberRole.LEADER) {
            return 5;
        }
        return 1;
    }

    // TODO: 현재 Mock 데이터. Submission 구현되면 리더는 한 명이라도 안 낸 과제, 멤버는 자신이 제출하지 않은 과제 개수로 변경
    private int unFinishedAssignmentCount(StudyMemberRole role) {
        if (role == StudyMemberRole.LEADER) {
            return 5;
        }
        return 1;
    }

    public StudyInviteLinkResponse getInviteLink(Long userId, Long studyId) {
        studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        return new StudyInviteLinkResponse(studyInviteLinkGenerator.generate(studyId));
    }
}
