import { render, screen, waitFor } from '@testing-library/react';
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

describe('프로필 이미지 변경', () => {
  beforeEach(() => {
    setAccessToken('1');
    server.use(
      http.get(`${API_URL}${USER_URLS.me}`, () =>
        HttpResponse.json({ name: '이든', profileImageUrl: 'https://example.com/old.png' }),
      ),
    );
  });
  afterEach(() => clearAccessToken());

  test('업로드 완료 후 파일 ID로 이미지를 변경한다', async () => {
    const steps: string[] = [];
    server.use(
      http.post(`${API_URL}/files/presigned-url`, () => {
        steps.push('presign');
        return HttpResponse.json({
          fileId: 123,
          expiresAt: '2026-11-12T12:00:00Z',
          uploadUrl: 'https://upload.example.com/avatar',
        });
      }),
      http.put('https://upload.example.com/avatar', () => {
        steps.push('upload');
        return new HttpResponse(null, { status: 200 });
      }),
      http.post(`${API_URL}/files/123/complete`, () => {
        steps.push('complete');
        return new HttpResponse(null, { status: 204 });
      }),
      http.patch(`${API_URL}/users/me/profile-image`, async ({ request }) => {
        expect(await request.json()).toEqual({ fileId: 123 });
        steps.push('profile');
        return HttpResponse.json({ name: '이든', profileImageUrl: 'https://example.com/new.png' });
      }),
    );
    const { user } = renderMyPage();
    await screen.findByRole('button', { name: '내 프로필 이미지 변경' });
    await user.upload(
      screen.getByLabelText('프로필 이미지 파일'),
      new File(['image'], 'avatar.png', { type: 'image/png' }),
    );
    await screen.findByRole('img', { name: '이든님의 프로필 사진' });
    await waitFor(() =>
      expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
        'src',
        'https://example.com/new.png',
      ),
    );
    expect(steps).toEqual(['presign', 'upload', 'complete', 'profile']);
  });

  test('기본 이미지 선택 시 DELETE 요청 후 기본 이미지를 표시한다', async () => {
    server.use(
      http.delete(
        `${API_URL}/users/me/profile-image`,
        () => new HttpResponse(null, { status: 204 }),
      ),
    );
    const { user } = renderMyPage();
    await user.click(await screen.findByRole('button', { name: '내 프로필 이미지 변경' }));
    await user.click(screen.getByRole('button', { name: '기본 프로필 이미지 사용' }));
    await waitFor(() =>
      expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
        'src',
        'test-file-stub',
      ),
    );
  });

  test('초기화 실패 시 기존 이미지를 유지하고 오류를 표시한다', async () => {
    server.use(
      http.delete(
        `${API_URL}/users/me/profile-image`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );
    const { user } = renderMyPage();
    await user.click(await screen.findByRole('button', { name: '내 프로필 이미지 변경' }));
    await user.click(screen.getByRole('button', { name: '기본 프로필 이미지 사용' }));
    expect(
      await screen.findByText('프로필 이미지를 초기화하지 못했어요. 다시 시도해주세요.'),
    ).toBeVisible();
    expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      'https://example.com/old.png',
    );
  });
  test('5MB 초과 이미지는 업로드하지 않고 기존 이미지를 유지한다', async () => {
    const presign = jest.fn(() => new HttpResponse(null, { status: 500 }));
    server.use(http.post(`${API_URL}/files/presigned-url`, presign));
    const { user } = renderMyPage();
    await screen.findByRole('button', { name: '내 프로필 이미지 변경' });
    await user.upload(
      screen.getByLabelText('프로필 이미지 파일'),
      new File([new Uint8Array(5_242_881)], 'large.png', { type: 'image/png' }),
    );
    expect(await screen.findByText('파일당 최대 5MB까지 첨부할 수 있어요.')).toBeVisible();
    expect(presign).not.toHaveBeenCalled();
    expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      'https://example.com/old.png',
    );
  });

  test('업로드 실패 시 기존 이미지를 유지하고 다시 선택할 수 있다', async () => {
    server.use(
      http.post(`${API_URL}/files/presigned-url`, () => new HttpResponse(null, { status: 500 })),
    );
    const { user } = renderMyPage();
    await screen.findByRole('button', { name: '내 프로필 이미지 변경' });
    await user.upload(
      screen.getByLabelText('프로필 이미지 파일'),
      new File(['image'], 'avatar.png', { type: 'image/png' }),
    );
    expect(
      await screen.findByText('파일 업로드에 실패했어요. 삭제 후 다시 선택해주세요.'),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: '내 프로필 이미지 변경' })).toBeEnabled();
    expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      'https://example.com/old.png',
    );
  });
});
