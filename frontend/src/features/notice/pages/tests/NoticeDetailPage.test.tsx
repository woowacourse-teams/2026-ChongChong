import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

describe('리드 공지 삭제 실패', () => {
  beforeEach(() => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          role: 'LEADER',
          userName: '안톨리니',
        }),
      ),
      http.get(NOTICE_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '스터디 일정 안내',
          content: '이번 주 스터디는 토요일에 진행합니다.',
          createdAt: '2026-09-01T09:00:00',
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

  test('공지 삭제 권한이 없으면 권한 안내를 토스트로 표시하고 확인창을 닫는다', async () => {
    const user = userEvent.setup();
    server.use(
      http.delete(NOTICE_DETAIL_URL, () =>
        HttpResponse.json(
          { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeDetailPage();

    await user.click(await screen.findByRole('button', { name: '삭제' }));
    const dialog = screen.getByRole('alertdialog', { name: '공지를 삭제할까요?' });
    expect(dialog).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: '삭제' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('요청한 작업을 수행할 권한이 없습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
  });

  test('네트워크 에러가 발생하면 공지 삭제 실패 안내를 토스트로 표시하고 확인창을 닫는다', async () => {
    const user = userEvent.setup();
    server.use(http.delete(NOTICE_DETAIL_URL, () => HttpResponse.error()));
    renderNoticeDetailPage();

    await user.click(await screen.findByRole('button', { name: '삭제' }));
    const dialog = screen.getByRole('alertdialog', { name: '공지를 삭제할까요?' });
    expect(dialog).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: '삭제' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('공지 삭제에 실패했습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
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

describe('리드 공지 읽음 현황 조회 실패', () => {
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
      http.get(NOTICE_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '스터디 일정 안내',
          content: '이번 주 스터디는 토요일에 진행합니다.',
          createdAt: '2026-09-01T09:00:00',
        }),
      ),
    );
  });

  test('공지 읽음 현황을 조회할 권한이 없으면 본문에 권한 안내를 표시한다', async () => {
    server.use(
      http.get(`${NOTICE_DETAIL_URL}/status`, () =>
        HttpResponse.json(
          { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('요청한 작업을 수행할 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 공지 읽음 현황 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(`${NOTICE_DETAIL_URL}/status`, () => HttpResponse.error()));
    renderNoticeDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '공지 읽음 현황을 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });
});

describe('스터디원 내 공지 읽음 상태 조회 실패', () => {
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
      http.get(NOTICE_DETAIL_URL, () =>
        HttpResponse.json({
          id: 1,
          title: '스터디 일정 안내',
          content: '이번 주 스터디는 토요일에 진행합니다.',
          createdAt: '2026-09-01T09:00:00',
        }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(`${NOTICE_DETAIL_URL}/status/me`, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 내 공지 읽음 상태 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(`${NOTICE_DETAIL_URL}/status/me`, () => HttpResponse.error()));
    renderNoticeDetailPage();

    expect(
      await within(screen.getByRole('main')).findByText(
        '내 공지 읽음 상태를 불러오는데 실패했습니다.',
        {},
        { timeout: 3000 },
      ),
    ).toBeVisible();
  });
});
