import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../../config';
import { clearAccessToken, getAccessToken, setAccessToken } from '../../../login/accessToken';
import { memberTable } from '../../../member/mocks/db';
import { userTable } from '../../../user/mocks/db';
import { createWrapper } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { MYPAGE_URLS } from '../../urls';
import MyPage from '../MyPage';

function renderMyPage() {
  render(
    <Routes>
      <Route path="/studies/mypage" element={<MyPage />} />
      <Route path="/login" element={<h1>로그인 페이지</h1>} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/mypage'] }) },
  );
}

describe('프로필', () => {
  beforeEach(() => setAccessToken('1'));
  afterEach(() => clearAccessToken());

  test('로그인한 사용자의 이름과 프로필 이미지를 읽기 전용으로 표시한다', async () => {
    server.use(
      http.get(`${API_URL}${MYPAGE_URLS.me}`, () =>
        HttpResponse.json({ name: '이든', profileImageUrl: 'https://example.com/profile.webp' }),
      ),
    );
    renderMyPage();

    expect(await screen.findByRole('textbox', { name: '이름' })).toHaveValue('이든');
    expect(screen.getByRole('textbox', { name: '이름' })).toBeDisabled();
    expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      'https://example.com/profile.webp',
    );
    expect(screen.queryByRole('button', { name: '프로필 수정하기' })).not.toBeInTheDocument();
  });

  test('프로필 이미지가 없으면 기본 이미지를 표시한다', async () => {
    server.use(
      http.get(`${API_URL}${MYPAGE_URLS.me}`, () =>
        HttpResponse.json({ name: '이든', profileImageUrl: null }),
      ),
    );
    renderMyPage();

    expect(await screen.findByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      'test-file-stub',
    );
  });
});

describe('계정 메뉴', () => {
  beforeEach(() => setAccessToken('1'));
  afterEach(() => clearAccessToken());

  test('로그아웃하면 액세스 토큰을 지우고 로그인 페이지로 이동한다', async () => {
    const user = userEvent.setup();
    renderMyPage();

    await user.click(await screen.findByRole('button', { name: '로그아웃' }));

    expect(await screen.findByRole('heading', { name: '로그인 페이지' })).toBeVisible();
    expect(getAccessToken()).toBeNull();
  });

  test('회원 탈퇴를 확인하면 사용자 데이터를 삭제하고 로그인 페이지로 이동한다', async () => {
    const user = userEvent.setup();
    renderMyPage();

    await user.click(await screen.findByRole('button', { name: '회원 탈퇴' }));
    const dialog = screen.getByRole('alertdialog', { name: '회원 탈퇴하시겠습니까?' });
    expect(dialog).toBeVisible();

    await user.click(within(dialog).getByRole('button', { name: '탈퇴' }));

    expect(await screen.findByRole('heading', { name: '로그인 페이지' })).toBeVisible();
    expect(userTable.findFirst((query) => query.where({ id: 1 }))).toBeUndefined();
    expect(memberTable.findMany((query) => query.where({ userId: 1 }))).toHaveLength(0);
    expect(getAccessToken()).toBeNull();
  });

  test('회원 탈퇴를 취소하면 사용자 데이터를 유지한다', async () => {
    const user = userEvent.setup();
    renderMyPage();

    await user.click(await screen.findByRole('button', { name: '회원 탈퇴' }));
    const dialog = screen.getByRole('alertdialog', { name: '회원 탈퇴하시겠습니까?' });
    await user.click(within(dialog).getByRole('button', { name: '취소' }));

    await waitFor(() => expect(dialog).not.toBeVisible());
    expect(userTable.findFirst((query) => query.where({ id: 1 }))).toBeDefined();
    expect(getAccessToken()).toBe('1');
  });
});
