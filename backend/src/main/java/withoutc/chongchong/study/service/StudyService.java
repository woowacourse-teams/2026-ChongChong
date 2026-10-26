package withoutc.chongchong.study.service;

import static withoutc.chongchong.study.entity.Study.MAX_STUDY_MEMBER_COUNT;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.repository.projection.LeaderNoticeSummaryProjection;
import withoutc.chongchong.notification.service.NotificationService;
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
import withoutc.chongchong.study.controller.dto.StudyListResponse;
import withoutc.chongchong.study.controller.dto.StudySummaryResponse;
import withoutc.chongchong.study.controller.dto.StudyUpdateRequest;
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
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    static final int MAX_JOINED_STUDY_COUNT = 50;

    private final StudyRepository studyRepository;
    private final StudyImageService studyImageService;
    private final FileUrlGenerator fileUrlGenerator;

    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NoticeRepository noticeRepository;
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    private final StudyInviteLinkGenerator studyInviteLinkGenerator;
    private final StudyInviteTokenProvider studyInviteTokenProvider;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public StudyCreateResponse createStudy(Long userId, StudyCreateRequest request) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);
        validateStudyCountLimit(userId);

        FileMetadata file = studyImageService.prepareForCreate(userId, request.imageFileId());

        Study study = Study.create(request.name(), request.description(), request.isPublic());
        study.changeImage(file);
        study = studyRepository.save(study);

        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.LEADER);

        studyMemberRepository.save(studyMember);

        return StudyCreateResponse.from(study);
    }

    @Transactional
    public void updateStudy(Long userId, Long studyId, StudyUpdateRequest request) {
        Study study = studyRepository.getByIdForUpdateOrThrow(studyId);

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeaderRole(studyMember);

        FileMetadata file = studyImageService.prepareForUpdate(userId, study, request.imageFileId());

        study.update(request.name(), request.description(), request.isPublic());
        study.changeImage(file);
    }

    @Transactional
    public void deleteStudy(Long userId, Long studyId) {
        Study study = studyRepository.getByIdForUpdateOrThrow(studyId);

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeaderRole(studyMember);

        entityManager.clear();

        studyImageService.prepareForDelete(study);
        notificationService.deleteNotificationsForStudy(studyId);
        studyRepository.delete(study);
    }

    public StudyInfoResponse getStudyInfo(Long userId, Long studyId) {
        Study study = studyRepository.getByIdOrThrow(studyId);

        StudyMember studyMember = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        FileMetadata imageFile = study.getImageFile();
        String imageUrl = null;
        if (imageFile != null) {
            imageUrl = fileUrlGenerator.generateImageUrl(imageFile.getObjectKey(), imageFile.getContentType());
        }

        return new StudyInfoResponse(study.getName(), study.getDescription(), studyMember.getRole(),
                studyMember.getName(), study.isPublic(), imageUrl);
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
            String imageUrl = getImageUrl(study);

            int memberCount = studyMemberRepository.countByStudyId(study.getId());
            int noticeCount = unReadNoticeCount(studyMember, study.getId());
            int assignmentCount = unFinishedAssignmentCount(studyMember, study.getId());

            responses.add(MyStudyResponse.from(study, studyMember.getRole(), memberCount, noticeCount, assignmentCount,
                    imageUrl));
        }

        return new MyStudyListResponse(responses.size(), responses);
    }

    public StudyListResponse getPublicStudies(Long userId, String keyword, Long cursor, int size) {
        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);
        userRepository.getByIdOrThrow(userId);

        Pageable pageable = PageRequest.of(0, pageRequest.fetchSize());

        LocalDateTime cursorCreatedAt = null;
        if (pageRequest.cursor() != null) {
            Study cursorStudy = studyRepository.getByIdOrThrow(pageRequest.cursor());
            cursorCreatedAt = cursorStudy.getCreatedAt();
        }

        List<Study> studies;

        if (keyword == null) {
            studies = studyRepository.findByCursor(
                    pageRequest.cursor(), cursorCreatedAt, MAX_STUDY_MEMBER_COUNT, pageable);
        } else {
            studies = studyRepository.searchByNameAndCursor(
                    keyword, pageRequest.cursor(), cursorCreatedAt, MAX_STUDY_MEMBER_COUNT, pageable);
        }

        CursorPageResponse<Study> studyPage = CursorPageResponse.of(studies, pageRequest, Study::getId);
        List<StudySummaryResponse> studySummaries = createStudySummaries(studyPage.content());

        return new StudyListResponse(studyPage.nextCursor(), studyPage.hasNext(), studySummaries);
    }

    public StudySummaryResponse getStudyJoinInfo(Long userId, Long studyId, String token) {
        userRepository.getByIdOrThrow(userId);
        Long resolvedStudyId = resolveStudyIdForJoinInfo(studyId, token);
        Study study = studyRepository.getByIdOrThrow(resolvedStudyId);
        if (!study.isPublic() && token == null) {
            throw new StudyException(StudyErrorCode.STUDY_NOT_PUBLIC);
        }

        int memberCount = studyMemberRepository.countByStudyId(resolvedStudyId);

        return createStudySummary(study, memberCount);
    }

    public StudyInviteLinkResponse getInviteLink(Long userId, Long studyId) {
        studyRepository.getByIdOrThrow(studyId);

        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        return new StudyInviteLinkResponse(studyInviteLinkGenerator.generate(studyId));
    }

    private Long resolveStudyIdForJoinInfo(Long studyId, String token) {
        if (token == null) {
            if (studyId == null) {
                throw new StudyException(StudyErrorCode.INVALID_STUDY_ID);
            }
            return studyId;
        }

        Long invitedStudyId = studyInviteTokenProvider.verifyAndExtractStudyId(token);
        if (studyId != null && !studyId.equals(invitedStudyId)) {
            throw new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN);
        }
        return invitedStudyId;
    }

    private List<StudySummaryResponse> createStudySummaries(List<Study> studies) {
        List<StudySummaryResponse> response = new ArrayList<>();
        for (Study study : studies) {
            int memberCount = studyMemberRepository.countByStudyId(study.getId());
            response.add(createStudySummary(study, memberCount));
        }
        return response;
    }

    private StudySummaryResponse createStudySummary(Study study, int memberCount) {
        String imageUrl = getImageUrl(study);

        return new StudySummaryResponse(study.getId(), study.getName(), study.getDescription(), imageUrl, memberCount);
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

    private String getImageUrl(Study study) {
        FileMetadata imageFile = study.getImageFile();

        String imageUrl = null;
        if (imageFile != null) {
            imageUrl = fileUrlGenerator.generateImageUrl(imageFile.getObjectKey(), imageFile.getContentType());
        }
        return imageUrl;
    }
}
