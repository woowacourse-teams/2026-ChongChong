import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper, setup, login, logout } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import NoticeListPage from '../NoticeListPage';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const NOTICE_LIST_URL = `${API_URL}/studies/:studyId/notices`;

function renderNoticeListPage() {
  return setup(
    <Routes>
      <Route path="/studies/:studyId/notices" element={<NoticeListPage />} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/notices'] }) },
  );
}

describe('기본 정보 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('스터디에 대한 접근 권한이 없으면 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeListPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
  });

  test('네트워크 에러가 발생하면 스터디 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(STUDY_INFO_URL, () => HttpResponse.error()));
    renderNoticeListPage();

    expect(
      await screen.findByText('스터디 정보를 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
  });
});

describe('리드 공지 목록 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          isPublic: false,
          role: 'LEADER',
          userName: '안톨리니',
          description: '즐거운 객체지향 스터디',
        }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(NOTICE_LIST_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeListPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 공지 목록 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(NOTICE_LIST_URL, () => HttpResponse.error()));
    renderNoticeListPage();

    expect(
      await screen.findByText('공지 목록을 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('공지 목록을 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });

  test('공지 목록 조회에 실패해도 스터디 헤더와 하단 탭을 유지한다', async () => {
    server.use(
      http.get(NOTICE_LIST_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeListPage();

    await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.');

    const header = screen.getByRole('banner');
    expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
    expect(within(header).getByRole('heading', { name: '객체지향 스터디' })).toBeVisible();
    expect(within(header).getByText('안톨리니 · 리드')).toBeVisible();
    expect(screen.getByRole('navigation')).toBeVisible();
  });
});

describe('스터디원 공지 목록 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(STUDY_INFO_URL, () =>
        HttpResponse.json({
          studyName: '객체지향 스터디',
          isPublic: false,
          role: 'MEMBER',
          userName: '안톨리니',
          description: '즐거운 객체지향 스터디',
        }),
      ),
    );
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(NOTICE_LIST_URL, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderNoticeListPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 공지 목록 조회 실패 안내를 표시한다', async () => {
    server.use(http.get(NOTICE_LIST_URL, () => HttpResponse.error()));
    renderNoticeListPage();

    expect(
      await screen.findByText('공지 목록을 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('공지 목록을 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });
});

describe('Floating UI', () => {
  beforeEach(async () => {
    await userTable.create({ id: 1, name: '리드', profileImage: 'https://example.com/leader.png' });
    await userTable.create({
      id: 2,
      name: '스터디원',
      profileImage: 'https://example.com/member.png',
    });
    await studyTable.create({
      id: 1,
      name: '객체지향 스터디',
      description: '객체지향을 공부하는 스터디입니다',
      inviteLink: 'object-oriented',
    });
    await memberTable.create({
      id: 1,
      studyId: 1,
      userId: 1,
      name: '리드',
      profileImage: 'https://example.com/leader.png',
      role: 'LEADER',
    });
    await memberTable.create({
      id: 2,
      studyId: 1,
      userId: 2,
      name: '스터디원',
      profileImage: 'https://example.com/member.png',
      role: 'MEMBER',
    });
  });

  afterEach(() => {
    logout();
  });

  test('스터디원이 공지 목록에 들어오면 Floating 메뉴가 보이지 않는다', async () => {
    login('스터디원');
    renderNoticeListPage();

    expect(await screen.findByText('아직 공지가 없어요')).toBeVisible();
    expect(screen.queryByRole('button', { name: '공지 메뉴 열기' })).not.toBeInTheDocument();
  });

  test('리드가 Floating을 열면 공지 작성하기와 닫기 메뉴가 보인다', async () => {
    login('리드');
    const { user } = renderNoticeListPage();

    const floating = await screen.findByRole('button', { name: '공지 메뉴 열기' });

    await user.click(floating);

    const menu = within(screen.getByRole('dialog', { name: '공지 메뉴' }));
    await waitFor(() => expect(menu.getByRole('link', { name: /공지 작성하기/ })).toBeVisible());
    await waitFor(() => expect(menu.getByRole('button', { name: '닫기' })).toBeVisible());
  });
});
