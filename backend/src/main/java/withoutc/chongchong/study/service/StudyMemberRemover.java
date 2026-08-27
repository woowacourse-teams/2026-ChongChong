package withoutc.chongchong.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@Component
@RequiredArgsConstructor
class StudyMemberRemover {

    private final NotificationRepository notificationRepository;
    private final NoticeRecipientRepository noticeRecipientRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final StudyMemberRepository studyMemberRepository;

    void remove(StudyMember member) {
        Long memberId = member.getId();

        notificationRepository.deleteAllByRecipientId(memberId);
        noticeRecipientRepository.deleteAllByMemberId(memberId);
        assignmentSubmissionRepository.deleteAllByMemberId(memberId);
        studyMemberRepository.delete(member);
    }
}
