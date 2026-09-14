import type { PropsWithChildren } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import AssignmentDetailPage from '../AssignmentDetailPage';

// 조회 추적에서 사용하는 IntersectionObserver는 Jest 환경에 없어 래퍼만 대체한다.
jest.mock('@posthog/react', () => ({
  ...jest.requireActual('@posthog/react'),
  PostHogCaptureOnViewed: ({ children }: PropsWithChildren) => children,
}));

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const ASSIGNMENT_DETAIL_URL = `${API_URL}/studies/:studyId/assignments/:assignmentId`;

function renderAssignmentDetailPage() {
  render(
    <Routes>
      <Route
        path="/studies/:studyId/assignments/:assignmentId"
        element={<AssignmentDetailPage />}
      />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/assignments/1'] }) },
  );
}

describe('리드 과제 상세 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'LEADER',
          userName: '안톨리니',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/status`, () =>
        HttpResponse.json({
          id: 1,
          memberCount: 1,
          completeCount: 0,
          incompleteCount: 1,
          completeMembers: [],
          incompleteMembers: [{ id: 1, name: '안톨리니', profileImage: null }],
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions`, () =>
        HttpResponse.json({ submissions: [] }),
      ),
    );
  });

  test('접근 권한 관련 에러가 발생하면 에러 메시지를 본문에 표시한다', async () => {
    server.use(
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '과제 정보를 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });

  test('조회에 실패해도 헤더와 하단 탭을 유지한다', async () => {
    server.use(http.get(ASSIGNMENT_DETAIL_URL, () => new HttpResponse(null, { status: 403 })));
    renderAssignmentDetailPage();

    await within(screen.getByRole('main')).findByRole('img', { name: '오류' });

    const header = screen.getByRole('banner');
    expect(within(header).getByRole('heading', { name: '과제' })).toBeVisible();
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});

describe('스터디원 과제 상세 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'MEMBER',
          userName: '안톨리니',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions/my`, () =>
        HttpResponse.json({ submitted: false }),
      ),
    );
  });

  test('접근 권한 관련 에러가 발생하면 에러 메시지를 본문에 표시한다', async () => {
    server.use(
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '과제 정보를 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });
});

describe('리드 과제 삭제 실패', () => {
  beforeEach(() => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'LEADER',
          userName: '안톨리니',
        }),
      ),
      http.get(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '객체지향 설계 과제',
          content: '객체의 역할과 책임을 정리해주세요.',
          submissionMethod: '텍스트로 제출하세요',
          closeAt: '2999-12-31T23:59:59',
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/status`, () =>
        HttpResponse.json({
          id: 1,
          memberCount: 1,
          completeCount: 0,
          incompleteCount: 1,
          completeMembers: [],
          incompleteMembers: [{ id: 1, name: '안톨리니', profileImage: null }],
        }),
      ),
      http.get(`${ASSIGNMENT_DETAIL_URL}/submissions`, () =>
        HttpResponse.json({ submissions: [] }),
      ),
    );
  });

  test('과제 삭제 권한이 없으면 권한 안내를 토스트로 표시하고 확인창을 닫는다', async () => {
    const user = userEvent.setup();
    server.use(
      http.delete(ASSIGNMENT_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentDetailPage();

    await user.click(await screen.findByRole('button', { name: '삭제' }));
    const dialog = screen.getByRole('alertdialog', { name: '과제를 삭제할까요?' });
    expect(dialog).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: '삭제' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('요청한 작업을 수행할 권한이 없습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
  });

  test('네트워크 에러가 발생하면 과제 삭제 실패 안내를 토스트로 표시하고 확인창을 닫는다', async () => {
    const user = userEvent.setup();
    server.use(http.delete(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()));
    renderAssignmentDetailPage();

    await user.click(await screen.findByRole('button', { name: '삭제' }));
    const dialog = screen.getByRole('alertdialog', { name: '과제를 삭제할까요?' });
    expect(dialog).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: '삭제' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('과제 삭제에 실패했습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
  });
});
