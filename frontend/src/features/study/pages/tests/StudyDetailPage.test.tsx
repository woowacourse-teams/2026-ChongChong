import { act, render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { createWrapper } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import StudyDetailPage from '../StudyDetailPage';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../urls';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const STUDY_DETAIL_URL = `${API_URL}${STUDY_URLS.detail}`;

function mockStudyResponses(role: 'LEADER' | 'MEMBER') {
  server.use(
    http.get(STUDY_INFO_URL, () =>
      HttpResponse.json({ studyName: '객체지향 스터디', role, userName: '안톨리니' }),
    ),
    http.get(STUDY_DETAIL_URL, () =>
      HttpResponse.json(
        role === 'LEADER'
          ? { notices: { count: 0, items: [] }, assignments: { count: 0, items: [] } }
          : { totalCount: 0, notices: { items: [] }, assignments: { items: [] } },
      ),
    ),
  );
}

function renderStudyDetailPage() {
  render(
    <Routes>
      <Route path="/studies/:studyId" element={<StudyDetailPage />} />
      <Route path="/studies" element={<h1>내 스터디</h1>} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1'] }) },
  );
}

describe('스터디 리드', () => {
  beforeEach(() => {
    mockStudyResponses('LEADER');
  });

  test('스터디 리드일 경우 헤더에 리드로 렌더링 한다', async () => {
    renderStudyDetailPage();

    expect(await screen.findByText('안톨리니 · 리드')).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});

describe('스터디원', () => {
  beforeEach(() => {
    mockStudyResponses('MEMBER');
  });

  test('스터디원일 경우 헤더에 스터디원으로 렌더링 한다', async () => {
    renderStudyDetailPage();

    expect(await screen.findByText('안톨리니 · 스터디원')).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});

describe('기본 정보 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('기본 정보를 불러오는 동안에도 하단 탭과 네비게이션을 표시한다', async () => {
    mockStudyResponses('MEMBER');
    let responseFinish!: () => void;
    const responseReady = new Promise<void>((resolve) => {
      responseFinish = resolve;
    });
    server.use(
      http.get(STUDY_INFO_URL, async () => {
        await responseReady;
        return HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'MEMBER',
          userName: '안톨리니',
        });
      }),
    );

    renderStudyDetailPage();

    expect(await screen.findByRole('img', { name: '로딩 중' })).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
    await act(async () => {
      responseFinish();
    });
    await screen.findByText('안톨리니 · 스터디원');
  });

  test('접근 권한이 없으면 오류 안내와 뒤로가기 헤더 및 하단 탭을 표시한다', async () => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json(
          {
            code: 'STUDY_ACCESS_DENIED',
            message: '해당 스터디에 대한 접근 권한이 없습니다.',
          },
          { status: 403 },
        ),
      ),
    );
    renderStudyDetailPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    const header = screen.getByRole('banner');
    const backButton = within(header).getByRole('button', { name: '뒤로 가기' });
    expect(backButton).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });

  test('네트워크 오류가 발생하면 기본 오류 안내와 뒤로가기 헤더 및 하단 탭을 표시한다', async () => {
    server.use(http.get(STUDY_INFO_URL, () => HttpResponse.error()));
    renderStudyDetailPage();

    expect(
      await screen.findByText('스터디 정보를 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    const header = screen.getByRole('banner');
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(within(header).queryByRole('heading')).not.toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeVisible();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});

describe('상세 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test.each([
    { role: 'LEADER', roleName: '리드' },
    { role: 'MEMBER', roleName: '스터디원' },
  ] as const)(
    '$roleName 상세 조회에 실패하면 본문에 오류를 표시하고 헤더와 하단 탭을 유지한다',
    async ({ role, roleName }) => {
      mockStudyResponses(role);
      server.use(http.get(STUDY_DETAIL_URL, () => HttpResponse.error()));
      renderStudyDetailPage();

      expect(await screen.findByText('스터디 정보를 불러오는데 실패했습니다.')).toBeVisible();

      const header = screen.getByRole('banner');
      expect(within(header).getByRole('heading', { name: '객체지향 스터디' })).toBeVisible();
      expect(within(header).getByText(`안톨리니 · ${roleName}`)).toBeVisible();
      expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
      expect(screen.getByRole('navigation')).toBeVisible();
    },
  );

  test.each([
    { role: 'LEADER', roleName: '리드' },
    { role: 'MEMBER', roleName: '스터디원' },
  ] as const)(
    '$roleName 이 현재 멤버가 아닐경우 본문에 오류를 표시하고 헤더와 하단 탭을 유지한다',
    async ({ role, roleName }) => {
      mockStudyResponses(role);
      server.use(
        http.get(STUDY_DETAIL_URL, () => {
          return HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          );
        }),
      );
      renderStudyDetailPage();

      expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();

      const header = screen.getByRole('banner');
      expect(within(header).getByRole('heading', { name: '객체지향 스터디' })).toBeVisible();
      expect(within(header).getByText(`안톨리니 · ${roleName}`)).toBeVisible();
      expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
      expect(screen.getByRole('navigation')).toBeVisible();
    },
  );
});
