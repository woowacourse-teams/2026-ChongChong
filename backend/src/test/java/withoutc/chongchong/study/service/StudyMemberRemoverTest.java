package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@ExtendWith(MockitoExtension.class)
class StudyMemberRemoverTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NoticeRecipientRepository noticeRecipientRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @InjectMocks
    private StudyMemberRemover studyMemberRemover;

    @Test
    @DisplayName("멤버 종속 데이터와 멤버를 순서대로 삭제한다")
    void removeTest() {
        Long memberId = 1L;
        StudyMember member = mock(StudyMember.class);
        when(member.getId()).thenReturn(memberId);

        studyMemberRemover.remove(member);

        InOrder inOrder = inOrder(
                notificationRepository,
                noticeRecipientRepository,
                assignmentSubmissionRepository,
                studyMemberRepository
        );
        inOrder.verify(notificationRepository).deleteAllByRecipientId(memberId);
        inOrder.verify(noticeRecipientRepository).deleteAllByMemberId(memberId);
        inOrder.verify(assignmentSubmissionRepository).deleteAllByMemberId(memberId);
        inOrder.verify(studyMemberRepository).delete(member);
    }

    @Test
    @DisplayName("종속 데이터 삭제에 실패하면 이후 삭제를 수행하지 않는다")
    void removeWithFailureTest() {
        Long memberId = 1L;
        StudyMember member = mock(StudyMember.class);
        when(member.getId()).thenReturn(memberId);
        when(noticeRecipientRepository.deleteAllByMemberId(memberId))
                .thenThrow(new IllegalStateException("delete failed"));

        assertThatThrownBy(() -> studyMemberRemover.remove(member))
                .isInstanceOf(IllegalStateException.class);

        verify(assignmentSubmissionRepository, never()).deleteAllByMemberId(memberId);
        verify(studyMemberRepository, never()).delete(member);
    }
}
