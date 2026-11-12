import { File } from 'node:buffer';
import profileIcon from '../../../../shared/assets/unknown-profile.svg';
import { userTable } from '../../mocks/db';
import { fileTable, MOCK_UPLOAD_ORIGIN } from '../../../../shared/modules/files/mocks/db';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { HttpResponse, http } from 'msw';
import { API_URL } from '../../../../../config';
import { clearAccessToken, setAccessToken } from '../../../login/accessToken';
import { createWrapper, login, logout } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { USER_URLS } from '../../urls';
import MyPage from '../MyPage';

function renderMyPage() {
  const user = userEvent.setup();
  const view = render(
    <Routes>
      <Route path="/studies/mypage" element={<MyPage />} />
      <Route path="/login" element={<h1>로그인 페이지</h1>} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/mypage'] }) },
  );

  return { user, ...view };
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

  async function selectProfileImage(user, file: File) {
    await user.click(await screen.findByRole('button', { name: '내 프로필 이미지 변경' }));
    await user.click(screen.getByRole('button', { name: '프로필 이미지 선택' }));
    await user.upload(screen.getByLabelText('프로필 이미지 파일'), file);
  }

  test('이미지를 선택하면 업로드한 이미지가 저장되고 다시 방문해도 표시된다', async () => {
    const { user, unmount } = renderMyPage();
    const file = new File(['profile-image'], 'avatar.png', { type: 'image/png' });

    await selectProfileImage(user, file);

    await waitFor(() => {
      const savedUser = userTable.findFirst((q) => q.where({ id: 1 }));
      expect(savedUser?.profileImage).not.toBe(originalImageUrl);
      expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
        'src',
        savedUser?.profileImage,
      );
    });
    const uploadedFile = fileTable.findFirst((q) => q.where({ originalFileName: file.name }));
    expect(uploadedFile).toMatchObject({
      status: 'COMPLETED',
      ownerUserId: 1,
      uploadedSize: file.size,
    });
    const imageUrl = `${MOCK_UPLOAD_ORIGIN}/images/${uploadedFile!.id}`;
    expect(userTable.findFirst((q) => q.where({ id: 1 }))?.profileImage).toBe(imageUrl);

    unmount();
    renderMyPage();
    expect(await screen.findByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      imageUrl,
    );
  });

  test('기본 이미지를 선택하면 저장된 이미지가 초기화되고 다시 방문해도 유지된다', async () => {
    const { user, unmount } = renderMyPage();
    await user.click(await screen.findByRole('button', { name: '내 프로필 이미지 변경' }));
    await user.click(screen.getByRole('button', { name: '기본 프로필 이미지 사용' }));
    await waitFor(() =>
      expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
        'src',
        profileIcon,
      ),
    );
    expect(userTable.findFirst((q) => q.where({ id: 1 }))?.profileImage).toBeNull();

    unmount();
    renderMyPage();
    expect(await screen.findByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      profileIcon,
    );
  });

  test('초기화 실패 시 기존 이미지를 유지하고 오류를 표시한다', async () => {
    server.use(
      http.delete(
        `${API_URL}${USER_URLS.profileImage}`,
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
      originalImageUrl,
    );
    expect(userTable.findFirst((q) => q.where({ id: 1 }))?.profileImage).toBe(originalImageUrl);
  });

  test('5MB 초과 이미지는 업로드하지 않고 기존 이미지를 유지한다', async () => {
    const { user } = renderMyPage();
    await selectProfileImage(
      user,
      new File([new Uint8Array(5_242_881)], 'large.png', { type: 'image/png' }),
    );
    expect(await screen.findByText('파일당 최대 5MB까지 첨부할 수 있어요.')).toBeVisible();
    expect(fileTable.findMany((q) => q.where({ ownerUserId: 1 }))).toHaveLength(0);
    expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      originalImageUrl,
    );
    expect(userTable.findFirst((q) => q.where({ id: 1 }))?.profileImage).toBe(originalImageUrl);
  });

  test('업로드 실패 시 기존 이미지를 유지하고 다시 선택하면 저장된다', async () => {
    server.use(
      http.post(`${API_URL}/files/presigned-url`, () => new HttpResponse(null, { status: 500 }), {
        once: true,
      }),
    );
    const { user } = renderMyPage();
    const file = new File(['profile-image'], 'avatar.png', { type: 'image/png' });
    await selectProfileImage(user, file);
    expect(
      await screen.findByText('파일 업로드에 실패했어요. 삭제 후 다시 선택해주세요.'),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: '내 프로필 이미지 변경' })).toBeEnabled();
    expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
      'src',
      originalImageUrl,
    );

    await selectProfileImage(user, file);
    await waitFor(() => {
      const savedUser = userTable.findFirst((q) => q.where({ id: 1 }));
      expect(savedUser?.profileImage).not.toBe(originalImageUrl);
      expect(screen.getByRole('img', { name: '이든님의 프로필 사진' })).toHaveAttribute(
        'src',
        savedUser?.profileImage,
      );
    });
    expect(
      screen.queryByText('파일 업로드에 실패했어요. 삭제 후 다시 선택해주세요.'),
    ).not.toBeInTheDocument();
  });
});
