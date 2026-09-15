import { render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import AssignmentListPage from '../AssignmentListPage';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const ASSIGNMENT_LIST_URL = `${API_URL}/studies/:studyId/assignments`;

function renderAssignmentListPage() {
  render(
    <Routes>
      <Route path="/studies/:studyId/assignments" element={<AssignmentListPage />} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/assignments'] }) },
  );
}

describe('기본 정보 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('접근 권한 에러가 발생하면 뒤로가기 헤더와 오류 안내 및 하단 탭을 표시한다', async () => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentListPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    const header = screen.getByRole('banner');
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(within(header).queryByRole('heading')).not.toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeVisible();
  });

  test('네트워크 에러가 발생하면 뒤로가기 헤더와 기본 오류 안내 및 하단 탭을 표시한다', async () => {
    server.use(http.get(STUDY_INFO_URL, () => HttpResponse.error()));
    renderAssignmentListPage();

    expect(
      await screen.findByText('스터디 정보를 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    const header = screen.getByRole('banner');
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(within(header).queryByRole('heading')).not.toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});

describe('리드 과제 목록 조회 실패', () => {
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
    );
  });

  test('접근 권한 에러가 발생하면 본문에 서버 메시지를 표시한다', async () => {
    server.use(
      http.get(ASSIGNMENT_LIST_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentListPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(ASSIGNMENT_LIST_URL, () => HttpResponse.error()));
    renderAssignmentListPage();

    expect(
      await screen.findByText('과제 목록을 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('과제 목록을 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });
});

describe('스터디원 과제 목록 조회 실패', () => {
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
    );
  });

  test('접근 권한 에러가 발생하면 본문에 서버 메시지를 표시한다', async () => {
    server.use(
      http.get(ASSIGNMENT_LIST_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderAssignmentListPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 기본 에러 메시지를 표시한다', async () => {
    server.use(http.get(ASSIGNMENT_LIST_URL, () => HttpResponse.error()));
    renderAssignmentListPage();

    expect(
      await screen.findByText('과제 목록을 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('과제 목록을 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });
});
