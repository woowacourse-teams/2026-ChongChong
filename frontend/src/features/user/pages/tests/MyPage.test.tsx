import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../../config';
import { clearAccessToken, setAccessToken } from '../../../login/accessToken';
import { createWrapper } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { USER_URLS } from '../../urls';
import MyPage from '../MyPage';

function renderMyPage() {
  const user = userEvent.setup();
  render(
    <Routes>
      <Route path="/studies/mypage" element={<MyPage />} />
      <Route path="/login" element={<h1>로그인 페이지</h1>} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/mypage'] }) },
  );

  return { user };
}

describe('프로필', () => {
  beforeEach(() => setAccessToken('1'));
  afterEach(() => clearAccessToken());

  test('로그인한 사용자의 이름과 프로필 이미지를 표시한다', async () => {
    server.use(
      http.get(`${API_URL}${USER_URLS.me}`, () =>
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
    expect(screen.getByRole('button', { name: '프로필 수정하기' })).toBeVisible();
  });

  test('프로필 이름을 수정하고 저장된 이름을 표시한다', async () => {
    const updateRequest = jest.fn(async ({ request }: { request: Request }) => {
      const body = await request.json();
      return HttpResponse.json({ name: (body as { name: string }).name, profileImageUrl: null });
    });
    server.use(
      http.get(`${API_URL}${USER_URLS.me}`, () =>
        HttpResponse.json({ name: '이든', profileImageUrl: null }),
      ),
      http.patch(`${API_URL}${USER_URLS.me}`, updateRequest),
    );
    const { user } = renderMyPage();

    await user.click(await screen.findByRole('button', { name: '프로필 수정하기' }));
    const nameInput = screen.getByRole('textbox', { name: '이름' });
    expect(nameInput).toBeEnabled();

    await user.clear(nameInput);
    await user.type(nameInput, '바니');
    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findByRole('button', { name: '프로필 수정하기' })).toBeVisible();
    expect(screen.getByRole('textbox', { name: '이름' })).toHaveValue('바니');
    expect(screen.getByRole('textbox', { name: '이름' })).toBeDisabled();
    expect(updateRequest).toHaveBeenCalledTimes(1);
  });

  test('프로필 이미지가 없으면 기본 이미지를 표시한다', async () => {
    server.use(
      http.get(`${API_URL}${USER_URLS.me}`, () =>
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
