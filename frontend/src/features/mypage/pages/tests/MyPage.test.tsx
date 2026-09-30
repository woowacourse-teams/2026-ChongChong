import { render, screen } from '@testing-library/react';
import { Route, Routes } from 'react-router';
import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../../config';
import { clearAccessToken, setAccessToken } from '../../../login/accessToken';
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
