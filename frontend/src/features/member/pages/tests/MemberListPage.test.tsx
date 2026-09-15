import { render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import MemberListPage from '../MemberListPage';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;

function renderMemberListPage() {
  render(
    <Routes>
      <Route path="/studies/:studyId/members" element={<MemberListPage />} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/members'] }) },
  );
}

describe('멤버 페이지 기본 정보 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderMemberListPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 스터디 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(STUDY_INFO_URL, () => HttpResponse.error()));
    renderMemberListPage();

    expect(
      await screen.findByText('스터디 정보를 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('스터디 정보를 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });

  test('기본 정보 조회에 실패해도 헤더와 하단 탭을 유지한다', async () => {
    server.use(http.get(STUDY_INFO_URL, () => new HttpResponse(null, { status: 403 })));
    renderMemberListPage();

    await screen.findByRole('img', { name: '오류' });

    const header = screen.getByRole('banner');
    expect(within(header).getByRole('heading', { name: '멤버' })).toBeVisible();
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});
