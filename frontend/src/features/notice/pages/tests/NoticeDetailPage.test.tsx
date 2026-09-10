import { render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import NoticeDetailPage from '../NoticeDetailPage';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const NOTICE_DETAIL_URL = `${API_URL}/studies/:studyId/notices/:noticeId`;

function renderNoticeDetailPage() {
  render(
    <Routes>
      <Route path="/studies/:studyId/notices/:noticeId" element={<NoticeDetailPage />} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/notices/1'] }) },
  );
}

describe('리드 공지 상세 조회 실패', () => {
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
      http.get(`${NOTICE_DETAIL_URL}/status`, () =>
        HttpResponse.json({
          id: 1,
          memberCount: 1,
          readCount: 0,
          unreadCount: 1,
          readMembers: [],
          unreadMembers: [{ id: 1, name: '안톨리니', profileImage: null }],
        }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(NOTICE_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeDetailPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 공지 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(NOTICE_DETAIL_URL, () => HttpResponse.error()));
    renderNoticeDetailPage();

    expect(
      await screen.findByText('공지 정보를 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('공지 정보를 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });

  test('공지 상세 조회에 실패해도 헤더와 하단 탭을 유지한다', async () => {
    server.use(http.get(NOTICE_DETAIL_URL, () => new HttpResponse(null, { status: 403 })));
    renderNoticeDetailPage();

    await screen.findByRole('img', { name: '오류' });

    const header = screen.getByRole('banner');
    expect(within(header).getByRole('heading', { name: '공지' })).toBeVisible();
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});

describe('스터디원 공지 상세 조회 실패', () => {
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
      http.get(`${NOTICE_DETAIL_URL}/status/me`, () =>
        HttpResponse.json({ isRead: false, readAt: null }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(NOTICE_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeDetailPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 공지 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(NOTICE_DETAIL_URL, () => HttpResponse.error()));
    renderNoticeDetailPage();

    expect(
      await screen.findByText('공지 정보를 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('공지 정보를 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });
});
