import { render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import AssignmentSubmissionDetailPage from '../AssignmentSubmissionDetailPage';

const ASSIGNMENT_SUBMISSION_DETAIL_URL = `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId`;

function renderAssignmentSubmissionDetailPage() {
  render(
    <Routes>
      <Route
        path="/studies/:studyId/assignments/:assignmentId/submissions/:submissionId"
        element={<AssignmentSubmissionDetailPage />}
      />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/assignments/1/submissions/1'] }) },
  );
}

describe('과제 제출물 상세 조회', () => {
  test('링크가 null인 제출 정보를 정상적으로 표시하고 링크 영역은 표시하지 않는다', async () => {
    server.use(
      http.get(ASSIGNMENT_SUBMISSION_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          name: '안톨리니',
          profileImage: null,
          createdAt: '2026-09-01T09:00:00',
          content: '링크 없이 제출한 내용',
          link: null,
        }),
      ),
    );
    renderAssignmentSubmissionDetailPage();

    const main = within(screen.getByRole('main'));
    expect(await main.findByText('링크 없이 제출한 내용')).toBeVisible();
    expect(main.queryByRole('link')).not.toBeInTheDocument();
  });
});

describe('과제 제출물 상세 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(ASSIGNMENT_SUBMISSION_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentSubmissionDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(ASSIGNMENT_SUBMISSION_DETAIL_URL, () => HttpResponse.error()));
    renderAssignmentSubmissionDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '제출 정보를 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });
});
