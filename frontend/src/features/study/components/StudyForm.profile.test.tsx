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

test('연속 선택에서는 마지막 업로드만 적용하고 완료 전에는 기존 이미지를 유지한다', async () => {
  let finishFirst!: (id: number) => void;
  let finishLast!: (id: number) => void;
  upload
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finishFirst = resolve;
        }),
    )
    .mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finishLast = resolve;
        }),
    );
  const { user, onSubmit } = renderForm('https://example.com/previous.png');
  const input = screen.getByLabelText('프로필 이미지 파일');
  await user.upload(input, new File(['one'], 'one.png', { type: 'image/png' }));
  expect(screen.getByAltText('스터디 프로필')).toHaveAttribute(
    'src',
    'https://example.com/previous.png',
  );
  expect(screen.getByRole('button', { name: '저장' })).toBeDisabled();
  await user.upload(input, new File(['two'], 'two.png', { type: 'image/png' }));
  await act(async () => finishLast(2));
  await act(async () => finishFirst(1));
  await user.click(screen.getByRole('button', { name: '저장' }));
  expect(onSubmit).toHaveBeenCalledWith({ name: '스터디', description: '', imageFileId: 2 });
  expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
});

test('기본 이미지로 되돌린 후 늦게 완료된 업로드를 무시한다', async () => {
  let finish!: (id: number) => void;
  upload.mockResolvedValueOnce(1).mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  const { user, onSubmit } = renderForm();
  const input = screen.getByLabelText('프로필 이미지 파일');
  await user.upload(input, new File(['one'], 'one.png', { type: 'image/png' }));
  await waitFor(() => expect(screen.getByRole('button', { name: '저장' })).toBeEnabled());
  await user.upload(input, new File(['two'], 'two.png', { type: 'image/png' }));
  await user.click(screen.getByRole('button', { name: '스터디 프로필 이미지 변경' }));
  await user.click(screen.getByRole('button', { name: '기본 프로필 이미지 사용' }));
  await act(async () => finish(2));
  await user.click(screen.getByRole('button', { name: '저장' }));
  expect(onSubmit).toHaveBeenCalledWith({ name: '스터디', description: '' });
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

test('메뉴에서 키보드 포커스가 순환하고 Escape로 닫는다', async () => {
  const { user } = renderForm('https://example.com/photo.png');
  const trigger = screen.getByRole('button', { name: '스터디 프로필 이미지 변경' });
  await user.click(trigger);
  await user.tab({ shift: true });
  expect(screen.getByRole('button', { name: '취소' })).toHaveFocus();
  await user.tab();
  expect(screen.getByRole('button', { name: '프로필 이미지 선택' })).toHaveFocus();
  await user.keyboard('{Escape}');
  expect(trigger).toHaveFocus();
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

test('디코딩 중 새 사진을 선택하면 이전 결과를 무시하고 해당 URL을 정리한다', async () => {
  let finishFirstDecode!: () => void;
  jest.mocked(HTMLImageElement.prototype.decode).mockImplementationOnce(
    () =>
      new Promise<void>((resolve) => {
        finishFirstDecode = resolve;
      }),
  );
  jest
    .mocked(URL.createObjectURL)
    .mockReturnValueOnce('blob:first')
    .mockReturnValueOnce('blob:last');
  upload.mockResolvedValueOnce(1).mockResolvedValueOnce(2);
  const { user, onSubmit } = renderForm('https://example.com/previous.png');
  const input = screen.getByLabelText('프로필 이미지 파일');
  await user.upload(input, new File(['one'], 'one.png', { type: 'image/png' }));
  await user.upload(input, new File(['two'], 'two.png', { type: 'image/png' }));
  await waitFor(() =>
    expect(screen.getByAltText('스터디 프로필')).toHaveAttribute('src', 'blob:last'),
  );
  await act(async () => finishFirstDecode());
  expect(screen.getByAltText('스터디 프로필')).toHaveAttribute('src', 'blob:last');
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:first');
  expect(URL.revokeObjectURL).not.toHaveBeenCalledWith('blob:last');
  await user.click(screen.getByRole('button', { name: '저장' }));
  expect(onSubmit).toHaveBeenCalledWith({ name: '스터디', description: '', imageFileId: 2 });
});
