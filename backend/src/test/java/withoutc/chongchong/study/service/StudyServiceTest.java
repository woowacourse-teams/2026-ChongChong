package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.dto.LeaderStudyDetailResponse;
import withoutc.chongchong.study.dto.MemberStudyDetailResponse;
import withoutc.chongchong.study.dto.MyStudyListResponse;
import withoutc.chongchong.study.dto.MyStudyListResponse.MyStudyResponse;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyDetailResponse;
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
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private StudyInviteLinkGenerator studyInviteLinkGenerator;

    @InjectMocks
    private StudyService studyService;

    @Test
    @DisplayName("내가 가입한 스터디 목록을 가입 순서와 집계 정보와 함께 반환한다")
    void getMyStudiesTest() {
        Long userId = 1L;
        User user = User.create("사용자", "profile-image-url");
        Study leaderStudy = mock(Study.class);
        Study memberStudy = mock(Study.class);
        when(leaderStudy.getId()).thenReturn(1L);
        when(leaderStudy.getName()).thenReturn("리더 스터디");
        when(leaderStudy.getDescription()).thenReturn("리더 스터디 설명");
        when(memberStudy.getId()).thenReturn(2L);
        when(memberStudy.getName()).thenReturn("멤버 스터디");
        when(memberStudy.getDescription()).thenReturn("멤버 스터디 설명");

        StudyMember leader = StudyMember.create(
                leaderStudy, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.LEADER);
        StudyMember member = StudyMember.create(
                memberStudy, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.MEMBER);
        when(studyMemberRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(leader, member));
        when(studyMemberRepository.countByStudyId(1L)).thenReturn(3);
        when(studyMemberRepository.countByStudyId(2L)).thenReturn(2);

        MyStudyListResponse response = studyService.getMyStudies(userId);

        assertThat(response.studyCount()).isEqualTo(2);
        assertThat(response.studies())
                .extracting(
                        MyStudyResponse::id,
                        MyStudyResponse::role,
                        MyStudyResponse::name,
                        MyStudyResponse::description,
                        MyStudyResponse::memberCount,
                        MyStudyResponse::noticeCount,
                        MyStudyResponse::assignmentCount
                )
                .containsExactly(
                        tuple(1L, StudyMemberRole.LEADER, "리더 스터디", "리더 스터디 설명", 3, 5, 5),
                        tuple(2L, StudyMemberRole.MEMBER, "멤버 스터디", "멤버 스터디 설명", 2, 1, 1)
                );
        verify(studyMemberRepository).countByStudyId(1L);
        verify(studyMemberRepository).countByStudyId(2L);
    }

    @Test
    @DisplayName("스터디를 생성하고 생성자를 리더로 등록한다")
    void createTest() {
        Long userId = 1L;
        StudyCreateRequest request = new StudyCreateRequest("자바 스터디", "매주 월요일에 진행한다.");
        User user = User.create("사용자", "profile-image-url");
        ArgumentCaptor<Study> studyCaptor = ArgumentCaptor.forClass(Study.class);
        ArgumentCaptor<StudyMember> studyMemberCaptor = ArgumentCaptor.forClass(StudyMember.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyRepository.save(any(Study.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        studyService.create(userId, request);

        verify(studyRepository).save(studyCaptor.capture());
        verify(studyMemberRepository).save(studyMemberCaptor.capture());
        Study study = studyCaptor.getValue();
        assertThat(study.getName()).isEqualTo("자바 스터디");
        assertThat(study.getDescription()).isEqualTo("매주 월요일에 진행한다.");

        StudyMember studyMember = studyMemberCaptor.getValue();
        assertThat(studyMember.getStudy()).isSameAs(study);
        assertThat(studyMember.getUser()).isSameAs(user);
        assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.LEADER);
        assertThat(studyMember.getName()).isEqualTo(user.getName());
        assertThat(studyMember.getProfileImageUrl()).isEqualTo(user.getProfileImageUrl());
    }

    @Test
    @DisplayName("가입한 스터디가 50개 이상이면 스터디를 생성할 수 없다")
    void createStudyWhenJoinedStudyCountLimitExceededTest() {
        Long userId = 1L;
        StudyCreateRequest request = new StudyCreateRequest("자바 스터디", "설명");
        User user = User.create("사용자", "profile-image-url");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyMemberRepository.countByUserId(userId)).thenReturn(50);

        assertThatThrownBy(() -> studyService.create(userId, request))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.JOINED_STUDY_LIMIT_EXCEEDED);

        verify(studyRepository, never()).save(any(Study.class));
        verify(studyMemberRepository, never()).save(any(StudyMember.class));
    }

    @Test
    @DisplayName("스터디 리더는 멤버 수와 공지·과제 완료 수를 포함한 상세 정보를 조회한다")
    void getStudyDetailForLeaderTest() {
        Long userId = 1L;
        Long studyId = 1L;
        Study study = mock(Study.class);
        User user = User.create("리더", "profile-image-url");
        StudyMember studyMember = StudyMember.create(
                study, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.LEADER);
        Notice notice = mock(Notice.class);
        Assignment assignment = mock(Assignment.class);
        when(study.getId()).thenReturn(studyId);
        when(notice.getId()).thenReturn(10L);
        when(notice.getTitle()).thenReturn("공지");
        when(assignment.getId()).thenReturn(20L);
        when(assignment.getTitle()).thenReturn("과제");
        when(studyMemberRepository.findByStudyIdAndUserId(studyId, userId))
                .thenReturn(Optional.of(studyMember));
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyMemberRepository.countByStudyId(studyId)).thenReturn(3);
        when(noticeRepository.findAllByStudyId(studyId)).thenReturn(List.of(notice));
        when(assignmentRepository.findAllByStudyId(studyId)).thenReturn(List.of(assignment));

        StudyDetailResponse response = studyService.getStudyDetail(userId, studyId);

        assertThat(response).isInstanceOf(LeaderStudyDetailResponse.class);
        LeaderStudyDetailResponse leaderResponse = (LeaderStudyDetailResponse) response;
        assertThat(leaderResponse.memberCount()).isEqualTo(3);
        assertThat(leaderResponse.notices().count()).isEqualTo(1);
        assertThat(leaderResponse.notices().items())
                .extracting(LeaderStudyDetailResponse.LeaderNoticeSummaryResponse::id,
                        LeaderStudyDetailResponse.LeaderNoticeSummaryResponse::title,
                        LeaderStudyDetailResponse.LeaderNoticeSummaryResponse::completeCount)
                .containsExactly(tuple(10L, "공지", 2));
        assertThat(leaderResponse.assignments().count()).isEqualTo(1);
        assertThat(leaderResponse.assignments().items())
                .extracting(LeaderStudyDetailResponse.LeaderAssignmentSummaryResponse::id,
                        LeaderStudyDetailResponse.LeaderAssignmentSummaryResponse::title,
                        LeaderStudyDetailResponse.LeaderAssignmentSummaryResponse::completeCount)
                .containsExactly(tuple(20L, "과제", 2));
    }

    @Test
    @DisplayName("스터디 멤버는 공지·과제 목록과 전체 미완료 개수를 포함한 상세 정보를 조회한다")
    void getStudyDetailForMemberTest() {
        Long userId = 1L;
        Long studyId = 1L;
        Study study = mock(Study.class);
        User user = User.create("멤버", "profile-image-url");
        StudyMember studyMember = StudyMember.create(
                study, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.MEMBER);
        Notice notice = mock(Notice.class);
        Assignment assignment = mock(Assignment.class);
        when(study.getId()).thenReturn(studyId);
        when(notice.getId()).thenReturn(10L);
        when(notice.getTitle()).thenReturn("공지");
        when(assignment.getId()).thenReturn(20L);
        when(assignment.getTitle()).thenReturn("과제");
        when(studyMemberRepository.findByStudyIdAndUserId(studyId, userId))
                .thenReturn(Optional.of(studyMember));
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(noticeRepository.findAllByStudyId(studyId)).thenReturn(List.of(notice));
        when(assignmentRepository.findAllByStudyId(studyId)).thenReturn(List.of(assignment));

        StudyDetailResponse response = studyService.getStudyDetail(userId, studyId);

        assertThat(response).isInstanceOf(MemberStudyDetailResponse.class);
        MemberStudyDetailResponse memberResponse = (MemberStudyDetailResponse) response;
        assertThat(memberResponse.totalCount()).isEqualTo(4);
        assertThat(memberResponse.notices().items())
                .extracting(MemberStudyDetailResponse.MemberNoticeSummaryResponse::id,
                        MemberStudyDetailResponse.MemberNoticeSummaryResponse::title)
                .containsExactly(tuple(10L, "공지"));
        assertThat(memberResponse.assignments().items())
                .extracting(MemberStudyDetailResponse.MemberAssignmentSummaryResponse::id,
                        MemberStudyDetailResponse.MemberAssignmentSummaryResponse::title)
                .containsExactly(tuple(20L, "과제"));
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 스터디 상세 정보를 조회할 수 없다")
    void getStudyDetailForNonMemberTest() {
        Long userId = 1L;
        Long studyId = 1L;
        when(studyMemberRepository.findByStudyIdAndUserId(studyId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getStudyDetail(userId, studyId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.NOT_STUDY_MEMBER);

        verifyNoInteractions(studyRepository, noticeRepository, assignmentRepository);
    }

    @Test
    @DisplayName("존재하는 스터디의 초대 링크를 반환한다")
    void getInviteLinkTest() {
        Long userId = 1L;
        Long studyId = 1L;
        User user = User.create("사용자", "profile-image-url");
        Study study = Study.create("자바 스터디", "설명");
        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.MEMBER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyMemberRepository.findByStudyIdAndUserId(any(), any()))
                .thenReturn(Optional.of(studyMember));
        when(studyInviteLinkGenerator.generate(studyId))
                .thenReturn("https://test.chongchong.app/join?token=invite-token");

        StudyInviteLinkResponse response = studyService.getInviteLink(userId, studyId);

        assertThat(response.inviteLink()).isEqualTo("https://test.chongchong.app/join?token=invite-token");
        verify(studyInviteLinkGenerator).generate(studyId);
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 초대 링크를 조회할 수 없다")
    void getInviteLinkForNonMemberTest() {
        Long userId = 1L;
        Long studyId = 1L;
        User user = User.create("사용자", "profile-image-url");
        Study study = Study.create("자바 스터디", "설명");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyRepository.findById(studyId)).thenReturn(Optional.of(study));
        when(studyMemberRepository.findByStudyIdAndUserId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getInviteLink(userId, studyId))
                .isInstanceOf(StudyMemberException.class)
                .extracting(exception -> ((StudyMemberException) exception).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.NOT_STUDY_MEMBER);

        verifyNoInteractions(studyInviteLinkGenerator);
    }

    @Test
    @DisplayName("존재하지 않는 스터디의 초대 링크를 요청하면 예외가 발생한다")
    void getInviteLinkForMissingStudyTest() {
        Long userId = 1L;
        Long studyId = 1L;
        User user = User.create("사용자", "profile-image-url");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studyRepository.findById(studyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyService.getInviteLink(userId, studyId))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.STUDY_NOT_FOUND);

        verifyNoInteractions(studyInviteLinkGenerator, studyMemberRepository);
    }
}
