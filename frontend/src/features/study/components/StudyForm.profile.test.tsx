import { act, screen, waitFor } from '@testing-library/react';
import { createWrapper, setup } from '../../../test/render';
import { uploadAttachment } from '../../../shared/modules/files/api';
import StudyForm from './StudyForm';

jest.mock('../../../shared/modules/files/api', () => ({ uploadAttachment: jest.fn() }));
const upload = jest.mocked(uploadAttachment);

beforeEach(() => {
  upload.mockReset();
  HTMLImageElement.prototype.decode = jest.fn().mockResolvedValue(undefined);
  URL.createObjectURL = jest.fn(() => 'blob:study-photo');
  URL.revokeObjectURL = jest.fn();
});

function renderForm(imageUrl?: string) {
  const onSubmit = jest.fn();
  return {
    onSubmit,
    ...setup(
      <StudyForm
        submitLabel="저장"
        onSubmit={onSubmit}
        isSubmitting={false}
        fieldErrors={{}}
        initialValues={{ name: '스터디', description: '', imageUrl }}
      />,
      { wrapper: createWrapper() },
    ),
  };
}

test('기본 프로필 메뉴는 선택과 취소를 제공하고 취소하면 트리거로 포커스를 돌린다', async () => {
  const { user, onSubmit } = renderForm();
  const trigger = screen.getByRole('button', { name: '스터디 프로필 이미지 변경' });
  await user.click(trigger);
  expect(screen.getByRole('dialog', { name: '프로필 이미지' })).toBeVisible();
  expect(screen.getByRole('button', { name: '프로필 이미지 선택' })).toBeVisible();
  expect(screen.queryByRole('button', { name: '기본 프로필 이미지 사용' })).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '취소' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(trigger).toHaveFocus();
  expect(onSubmit).not.toHaveBeenCalled();
});

test('업로드 성공 후 사진과 파일 ID를 반영하고 기본 프로필로 되돌릴 수 있다', async () => {
  upload.mockResolvedValue(42);
  const { user, onSubmit } = renderForm();
  await user.upload(
    screen.getByLabelText('프로필 이미지 파일'),
    new File(['photo'], 'photo.png', { type: 'image/png' }),
  );
  await waitFor(() =>
    expect(screen.getByAltText('스터디 프로필')).toHaveAttribute('src', 'blob:study-photo'),
  );
  await user.click(screen.getByRole('button', { name: '저장' }));
  expect(onSubmit).toHaveBeenLastCalledWith({ name: '스터디', description: '', imageFileId: 42 });
  await user.click(screen.getByRole('button', { name: '스터디 프로필 이미지 변경' }));
  await user.click(screen.getByRole('button', { name: '기본 프로필 이미지 사용' }));
  await user.click(screen.getByRole('button', { name: '저장' }));
  expect(onSubmit).toHaveBeenLastCalledWith({ name: '스터디', description: '' });
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:study-photo');
});

test('교체 업로드 실패 시 기존 이미지를 유지하고 토스트로 알린다', async () => {
  upload.mockRejectedValue(new Error('전송 실패'));
  const { user } = renderForm('https://example.com/previous.png');
  await user.upload(
    screen.getByLabelText('프로필 이미지 파일'),
    new File(['photo'], 'photo.png', { type: 'image/png' }),
  );
  expect(await screen.findByRole('status')).toHaveTextContent('전송 실패');
  expect(screen.getByAltText('스터디 프로필')).toHaveAttribute(
    'src',
    'https://example.com/previous.png',
  );
  expect(screen.getByRole('button', { name: '저장' })).toBeEnabled();
});

test('업로드 중에는 프로필 메뉴와 파일 선택을 막고 완료 후 다시 활성화한다', async () => {
  let finish!: (id: number) => void;
  upload.mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  const { user } = renderForm('https://example.com/previous.png');
  const input = screen.getByLabelText('프로필 이미지 파일');
  const trigger = screen.getByRole('button', { name: '스터디 프로필 이미지 변경' });
  await user.upload(input, new File(['one'], 'one.png', { type: 'image/png' }));
  expect(trigger).toBeDisabled();
  expect(input).toBeDisabled();
  expect(screen.getByRole('button', { name: '저장' })).toBeDisabled();
  await user.click(trigger);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  await user.upload(input, new File(['two'], 'two.png', { type: 'image/png' }));
  expect(upload).toHaveBeenCalledTimes(1);
  expect(screen.getByAltText('스터디 프로필')).toHaveAttribute(
    'src',
    'https://example.com/previous.png',
  );
  await act(async () => finish(1));
  expect(trigger).toBeEnabled();
  expect(input).toBeEnabled();
});

test('화면을 떠난 후 업로드가 끝나면 미리보기를 생성하지 않는다', async () => {
  let finish!: (id: number) => void;
  upload.mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  const { user, unmount } = renderForm();
  await user.upload(
    screen.getByLabelText('프로필 이미지 파일'),
    new File(['one'], 'one.png', { type: 'image/png' }),
  );
  unmount();
  await act(async () => finish(1));
  expect(URL.createObjectURL).not.toHaveBeenCalled();
});

test('5MB를 넘는 이미지는 업로드하지 않고 토스트를 표시한다', async () => {
  const { user } = renderForm();
  await user.upload(
    screen.getByLabelText('프로필 이미지 파일'),
    new File([new Uint8Array(5_242_881)], 'large.png', { type: 'image/png' }),
  );
  expect(screen.getByRole('status')).toHaveTextContent('최대 5MB');
  expect(upload).not.toHaveBeenCalled();
});

test('새 이미지 디코딩이 끝날 때까지 기존 사진을 표시한다', async () => {
  let finishDecode!: () => void;
  jest.mocked(HTMLImageElement.prototype.decode).mockImplementationOnce(
    () =>
      new Promise<void>((resolve) => {
        finishDecode = resolve;
      }),
  );
  upload.mockResolvedValue(42);
  const { user } = renderForm('https://example.com/previous.png');
  await user.upload(
    screen.getByLabelText('프로필 이미지 파일'),
    new File(['photo'], 'photo.png', { type: 'image/png' }),
  );
  expect(screen.getByAltText('스터디 프로필')).toHaveAttribute(
    'src',
    'https://example.com/previous.png',
  );
  expect(screen.getByRole('button', { name: '저장' })).toBeDisabled();
  await act(async () => finishDecode());
  expect(screen.getByAltText('스터디 프로필')).toHaveAttribute('src', 'blob:study-photo');
  expect(screen.getByRole('button', { name: '저장' })).toBeEnabled();
});

test('읽을 수 없는 이미지이면 기존 사진을 유지하고 미리보기 URL을 정리한다', async () => {
  jest.mocked(HTMLImageElement.prototype.decode).mockRejectedValueOnce(new Error('decode failed'));
  upload.mockResolvedValue(42);
  const { user } = renderForm('https://example.com/previous.png');
  await user.upload(
    screen.getByLabelText('프로필 이미지 파일'),
    new File(['broken'], 'broken.png', { type: 'image/png' }),
  );
  expect(await screen.findByRole('status')).toHaveTextContent('이미지를 불러오지 못했어요');
  expect(screen.getByAltText('스터디 프로필')).toHaveAttribute(
    'src',
    'https://example.com/previous.png',
  );
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:study-photo');
});

test('디코딩 중 화면을 떠나면 준비 중이던 미리보기 URL을 정리한다', async () => {
  let finishDecode!: () => void;
  jest.mocked(HTMLImageElement.prototype.decode).mockImplementationOnce(
    () =>
      new Promise<void>((resolve) => {
        finishDecode = resolve;
      }),
  );
  upload.mockResolvedValueOnce(1);
  const { user, unmount } = renderForm();
  await user.upload(
    screen.getByLabelText('프로필 이미지 파일'),
    new File(['one'], 'one.png', { type: 'image/png' }),
  );
  expect(screen.getByRole('button', { name: '스터디 프로필 이미지 변경' })).toBeDisabled();
  unmount();
  await act(async () => finishDecode());
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:study-photo');
});

test.each(['배경 클릭', 'Escape'])(
  '%s으로 프로필 메뉴를 닫으면 포커스가 돌아온다',
  async (action) => {
    const { user, onSubmit } = renderForm();
    const trigger = screen.getByRole('button', { name: '스터디 프로필 이미지 변경' });
    await user.click(trigger);
    expect(screen.getByRole('dialog', { name: '프로필 이미지' })).toBeVisible();
    if (action === '배경 클릭') {
      await user.click(screen.getByTestId('bottom-sheet-backdrop'));
    } else {
      await user.keyboard('{Escape}');
    }
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
    expect(onSubmit).not.toHaveBeenCalled();
  },
);
