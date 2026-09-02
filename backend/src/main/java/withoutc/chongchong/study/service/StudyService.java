package withoutc.chongchong.study.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection;
import withoutc.chongchong.study.controller.dto.LeaderStudyDetailResponse;
import withoutc.chongchong.study.controller.dto.LeaderStudyDetailResponse.LeaderAssignmentSummaryListResponse;
import withoutc.chongchong.study.controller.dto.LeaderStudyDetailResponse.LeaderAssignmentSummaryResponse;
import withoutc.chongchong.study.controller.dto.LeaderStudyDetailResponse.LeaderNoticeSummaryListResponse;
import withoutc.chongchong.study.controller.dto.LeaderStudyDetailResponse.LeaderNoticeSummaryResponse;
import withoutc.chongchong.study.controller.dto.MemberStudyDetailResponse;
import withoutc.chongchong.study.controller.dto.MemberStudyDetailResponse.MemberAssignmentSummaryListResponse;
import withoutc.chongchong.study.controller.dto.MemberStudyDetailResponse.MemberAssignmentSummaryResponse;
import withoutc.chongchong.study.controller.dto.MemberStudyDetailResponse.MemberNoticeSummaryListResponse;
import withoutc.chongchong.study.controller.dto.MemberStudyDetailResponse.MemberNoticeSummaryResponse;
import withoutc.chongchong.study.controller.dto.MyStudyListResponse;
import withoutc.chongchong.study.controller.dto.MyStudyListResponse.MyStudyResponse;
import withoutc.chongchong.study.controller.dto.StudyCreateRequest;
import withoutc.chongchong.study.controller.dto.StudyCreateResponse;
import withoutc.chongchong.study.controller.dto.StudyDetailResponse;
import withoutc.chongchong.study.controller.dto.StudyInfoResponse;
import withoutc.chongchong.study.controller.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.controller.dto.StudyUpdateRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public StudyCreateResponse createStudy(Long userId, StudyCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        validateStudyCountLimit(userId);

        Study study = studyRepository.save(Study.create(request.name(), request.description()));

        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.LEADER);

        studyMemberRepository.save(studyMember);

        return StudyCreateResponse.from(study);
    }

    @Transactional
    public void updateStudy(Long userId, Long studyId, StudyUpdateRequest request) {
        Study study = studyRepository.getByIdOrThrow(studyId);

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeaderRole(studyMember);

        study.update(request.name(), request.description());
    }

    @Transactional
    public void deleteStudy(Long userId, Long studyId) {
        Study study = studyRepository.getByIdOrThrow(studyId);

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeaderRole(studyMember);

        entityManager.clear();
        studyRepository.delete(study);
    }

    public StudyInfoResponse getStudyInfo(Long userId, Long studyId) {
        Study study = studyRepository.getByIdOrThrow(studyId);

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        return new StudyInfoResponse(study.getName(), studyMember.getRole(), studyMember.getName());
    }

    public StudyDetailResponse getStudyDetail(Long userId, Long studyId) {
        Study study = studyRepository.getByIdOrThrow(studyId);

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        if (studyMember.getRole() == StudyMemberRole.LEADER) {
            return makeLeaderStudyDetailResponse(study);
        }

        return makeMemberStudyDetailResponse(study, studyMember);
    }

    public MyStudyListResponse getMyStudies(Long userId) {
        List<StudyMember> studyMembers = studyMemberRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        List<MyStudyResponse> responses = new ArrayList<>();
        for (StudyMember studyMember : studyMembers) {
            Study study = studyMember.getStudy();

            int memberCount = studyMemberRepository.countByStudyId(study.getId());
            int noticeCount = unReadNoticeCount(studyMember, study.getId());
            int assignmentCount = unFinishedAssignmentCount(studyMember, study.getId());

            responses.add(
                    MyStudyResponse.from(study, studyMember.getRole(), memberCount, noticeCount, assignmentCount));
        }

        return new MyStudyListResponse(responses.size(), responses);
    }

    public StudyInviteLinkResponse getInviteLink(Long userId, Long studyId) {
        studyRepository.getByIdOrThrow(studyId);

        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        return new StudyInviteLinkResponse(studyInviteLinkGenerator.generate(studyId));
    }

    private void validateStudyCountLimit(Long userId) {
        if (studyMemberRepository.countByUserId(userId) >= MAX_JOINED_STUDY_COUNT) {
            throw new StudyMemberException(StudyMemberErrorCode.JOINED_STUDY_LIMIT_EXCEEDED);
        }
    }

    private void validateLeaderRole(StudyMember studyMember) {
        if (studyMember.getRole() != StudyMemberRole.LEADER) {
            throw new StudyMemberException(StudyMemberErrorCode.NOT_STUDY_LEADER);
        }
    }

    private LeaderStudyDetailResponse makeLeaderStudyDetailResponse(Study study) {
        List<LeaderNoticeSummaryProjection> noticeProjections = noticeRepository.findIncompleteNoticeSummariesByStudyId(
                study.getId());
        List<LeaderAssignmentSummaryProjection> assignmentProjections = assignmentRepository.findIncompleteAssignmentSummariesByStudyId(
                study.getId());

        List<LeaderNoticeSummaryResponse> noticeResponses = new ArrayList<>();
        List<LeaderAssignmentSummaryResponse> assignmentResponses = new ArrayList<>();

        for (LeaderNoticeSummaryProjection projection : noticeProjections) {
            LeaderNoticeSummaryResponse response = new LeaderNoticeSummaryResponse(
                    projection.id(),
                    projection.title(),
                    (int) projection.memberCount(),
                    (int) projection.completeCount());
            noticeResponses.add(response);
        }

        for (LeaderAssignmentSummaryProjection projection : assignmentProjections) {
            LeaderAssignmentSummaryResponse response = new LeaderAssignmentSummaryResponse(
                    projection.id(),
                    projection.title(),
                    (int) projection.memberCount(),
                    (int) projection.completeCount());
            assignmentResponses.add(response);
        }

        return new LeaderStudyDetailResponse(LeaderNoticeSummaryListResponse.from(noticeResponses),
                LeaderAssignmentSummaryListResponse.from(assignmentResponses));
    }

    private MemberStudyDetailResponse makeMemberStudyDetailResponse(Study study, StudyMember studyMember) {
        List<Notice> notices = noticeRepository.findIncompleteNoticesByStudyIdAndMemberId(study.getId(),
                studyMember.getId());
        List<Assignment> assignments = assignmentRepository.findIncompleteAssignmentsByStudyIdAndMemberId(study.getId(),
                studyMember.getId());

        int totalCount = notices.size() + assignments.size();

        List<MemberNoticeSummaryResponse> noticeResponses = notices.stream()
                .map(notice -> new MemberNoticeSummaryResponse(notice.getId(), notice.getTitle()))
                .toList();

        List<MemberAssignmentSummaryResponse> assignmentResponses = assignments.stream()
                .map(assignment -> new MemberAssignmentSummaryResponse(assignment.getId(), assignment.getTitle()))
                .toList();

        return new MemberStudyDetailResponse(totalCount, MemberNoticeSummaryListResponse.from(noticeResponses),
                MemberAssignmentSummaryListResponse.from(assignmentResponses));
    }

    private int unReadNoticeCount(StudyMember studyMember, Long studyId) {
        if (studyMember.getRole() == StudyMemberRole.LEADER) {
            return (int) noticeRepository.countIncompleteNoticeByStudyId(studyId);
        }
        return (int) noticeRepository.countIncompleteNoticeByStudyIdAndMemberId(studyId, studyMember.getId());
    }

    private int unFinishedAssignmentCount(StudyMember studyMember, Long studyId) {
        if (studyMember.getRole() == StudyMemberRole.LEADER) {
            return (int) assignmentRepository.countIncompleteAssignmentByStudyId(studyId);
        }
        return (int) assignmentRepository.countIncompleteAssignmentByStudyIdAndMemberId(studyId, studyMember.getId());
    }
}
