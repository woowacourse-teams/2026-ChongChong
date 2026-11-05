import { assignmentTable, submissionTable } from '../../assignment/mocks/db';
import { mockMembers } from '../../member/mocks/db';
import { noticeRecipientTable, noticeTable } from '../../notice/mocks/db';
import { formatDate } from '../../../shared/ui/date-time-picker/dateTimePicker.utils';
import { getKoreanToday } from '../utils';

export function createSeedCalendar() {
  const studyId = 1;
  const noticeId = 10_001;
  const assignmentId = 10_001;
  const today = formatDate(getKoreanToday()).replaceAll('.', '-');
  const members = mockMembers.filter((member) => member.studyId === studyId);

  noticeTable.create({
    id: noticeId,
    studyId,
    title: '이번 주 스터디 진행 안내',
    content: '이번 주 모임에서 실습 과제를 함께 리뷰합니다.',
    createdAt: `${today}T09:00:00`,
  });
  assignmentTable.create({
    id: assignmentId,
    studyId,
    title: '스프링 실습 과제',
    content: '예제 프로젝트의 실행 결과를 정리해주세요.',
    submissionMethod: 'GitHub 링크로 제출하세요.',
    createdAt: `${today}T10:00:00`,
    closeAt: `${today}T23:59:00`,
    submissionTarget: 'MEMBERS_AND_LEADER',
    completeUserIds: [],
  });

  for (const { id, role } of members) {
    if (role !== 'MEMBER') continue;

    noticeRecipientTable.create({
      id: 10_000 + id,
      noticeId,
      memberId: id,
      readAt: null,
      lastRemindAt: null,
    });
  }

  for (const { id, userId } of members) {
    submissionTable.create({
      id: 10_000 + id,
      assignmentId,
      userId,
      assigned: true,
      submitted: false,
    });
  }
}
