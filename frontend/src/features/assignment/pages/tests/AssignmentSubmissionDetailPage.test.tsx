import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { memberTable } from '../../../member/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { userTable } from '../../../user/mocks/db';
import { assignmentTable, submissionTable } from '../../mocks/db';
import AssignmentSubmissionDetailPage from '../AssignmentSubmissionDetailPage';

const ASSIGNMENT_SUBMISSION_DETAIL_URL = `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId`;

function setupAssignmentSubmissionDetailPage() {
  return setup(<AssignmentSubmissionDetailPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/assignments/1/submissions/1'],
      routes: (element) => (
        <Route
          path="/studies/:studyId/assignments/:assignmentId/submissions/:submissionId"
          element={element}
        />
      ),
    }),
  });
}

describe('과제 제출물 상세 페이지 테스트', () => {
  const memberUserName = '디움';

  beforeEach(async () => {
    await userTable.create({
      id: 1,
      name: memberUserName,
      profileImage: 'http://localhost:8000',
    });
    await studyTable.create({
      id: 1,
      name: '축구 스터디',
      description: '축구 연습 내용을 공유하는 스터디',
      inviteLink: 'football',
    });
    await memberTable.create({
      id: 1,
      studyId: 1,
      userId: 1,
      name: memberUserName,
      profileImage: 'http://localhost:8000',
      role: 'MEMBER',
    });
  });

  afterEach(() => {
    logout();
  });

  describe('제출 정보 조회', () => {
    beforeEach(() => {
      login(memberUserName);
    });

    test('링크가 null인 제출 정보를 정상적으로 표시하고 링크 영역은 표시하지 않는다', async () => {
      await assignmentTable.create({
        id: 1,
        studyId: 1,
        title: '슈팅 연습',
        content: '슈팅 연습 내용을 작성해주세요.',
        submissionMethod: '텍스트로 제출하세요',
        closeAt: '2026-09-30T23:59:59',
        submissionTarget: 'MEMBERS_AND_LEADER',
        completeUserIds: [1],
      });
      await submissionTable.create({
        id: 1,
        assignmentId: 1,
        userId: 1,
        submitted: true,
        createdAt: '2026-09-01T09:00:00',
        content: '링크 없이 제출한 내용',
        link: null,
      });
      setupAssignmentSubmissionDetailPage();

      const main = within(screen.getByRole('main'));
      expect(await main.findByText('링크 없이 제출한 내용')).toBeVisible();
      expect(main.getByRole('heading', { name: memberUserName })).toBeVisible();
      expect(main.queryByRole('link')).not.toBeInTheDocument();
    });

    test.each([
      {
        title: '스터디에 대한 접근 권한이 없으면',
        handler: http.get(ASSIGNMENT_SUBMISSION_DETAIL_URL, () =>
          HttpResponse.json(
            { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '네트워크 에러가 발생하면',
        handler: http.get(ASSIGNMENT_SUBMISSION_DETAIL_URL, () => HttpResponse.error()),
        message: '제출 정보를 불러오는데 실패했습니다.',
      },
    ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
      jest.spyOn(console, 'error').mockImplementation(() => {});
      server.use(handler);
      setupAssignmentSubmissionDetailPage();

      expect(
        await within(screen.getByRole('main')).findByText(message, {}, { timeout: 3000 }),
      ).toBeVisible();
    });
  });
});
