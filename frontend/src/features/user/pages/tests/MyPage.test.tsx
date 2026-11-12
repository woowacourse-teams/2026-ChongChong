import { File } from 'node:buffer';
import profileIcon from '../../../../shared/assets/unknown-profile.svg';
import { userTable } from '../../mocks/db';
import { fileTable, MOCK_UPLOAD_ORIGIN } from '../../../../shared/modules/files/mocks/db';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../../config';
import { clearAccessToken, setAccessToken } from '../../../login/accessToken';
import { createWrapper, login, logout } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { USER_URLS } from '../../urls';
import MyPage from '../MyPage';

function renderMyPage() {
  const user = userEvent.setup({ applyAccept: false });
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
  const originalImageUrl = 'https://example.com/old.png';

  beforeEach(async () => {
    await userTable.create({ id: 1, name: '이든', profileImage: originalImageUrl });
    login('이든');
  });
  afterEach(() => logout());

  async function selectProfileImage(user: UserEvent, file: File) {
    await user.click(await screen.findByRole('button', { name: '내 프로필 이미지 변경' }));
    await user.click(screen.getByRole('button', { name: '프로필 이미지 선택' }));
    await user.upload(screen.getByLabelText('프로필 이미지 파일'), file);
  }

  test('이미지를 선택하면 업로드한 이미지가 저장되고 표시된다', async () => {
    const { user } = renderMyPage();
    const file = new File(['profile-image'], 'avatar.png', { type: 'image/png' });

    await selectProfileImage(user, file);

    await waitFor(() => {
      const uploadedFile = fileTable.findFirst((q) => q.where({ originalFileName: file.name }));
      expect(uploadedFile?.status).toBe('COMPLETED');
      const imageUrl = `${MOCK_UPLOAD_ORIGIN}/images/${uploadedFile?.id}`;
      expect(userTable.findFirst((q) => q.where({ id: 1 }))?.profileImage).toBe(imageUrl);
      expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
        'src',
        imageUrl,
      );
    });
  });

  test('이미지를 초기화하면 기본 이미지가 표시된다', async () => {
    const { user } = renderMyPage();

    await user.click(await screen.findByRole('button', { name: '내 프로필 이미지 변경' }));
    await user.click(screen.getByRole('button', { name: '기본 프로필 이미지 사용' }));

    await waitFor(() =>
      expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
        'src',
        profileIcon,
      ),
    );
    expect(userTable.findFirst((q) => q.where({ id: 1 }))?.profileImage).toBeNull();
  });

  test.each([
    {
      title: '5MB를 초과하는 이미지를',
      createFile: () => new File([new Uint8Array(5_242_881)], 'large.png', { type: 'image/png' }),
      message: '파일당 최대 5MB까지 첨부할 수 있어요.',
    },
    {
      title: '비어 있는 이미지를',
      createFile: () => new File([], 'empty.png', { type: 'image/png' }),
      message: '비어 있는 이미지는 첨부할 수 없어요.',
    },
    {
      title: '허용하지 않는 확장자의 파일을',
      createFile: () => new File(['image'], 'avatar.gif', { type: 'image/gif' }),
      message: 'JPEG, PNG, WebP 이미지만 첨부할 수 있어요.',
    },
    {
      title: '확장자와 MIME 타입이 다른 이미지를',
      createFile: () => new File(['image'], 'avatar.png', { type: 'image/jpeg' }),
      message: 'JPEG, PNG, WebP 이미지만 첨부할 수 있어요.',
    },
  ])('$title 업로드하면 에러 텍스트가 표시된다', async ({ createFile, message }) => {
    const { user } = renderMyPage();

    await selectProfileImage(user, createFile());

    expect(await screen.findByText(message)).toBeVisible();
  });
});
