import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import NoticeForm from '../NoticeForm';

describe('NoticeForm 테스트', () => {
  test('리마인드 시각을 등록해 제출하고 삭제해 취소할 수 있다', async () => {
    const user = userEvent.setup();
    const onSubmit = jest.fn();
    render(
      <NoticeForm
        initialValues={{ title: '공지', content: '내용' }}
        submitLabel="수정하기"
        onSubmit={onSubmit}
      />,
    );

    await user.click(screen.getByRole('button', { name: '리마인드 시각' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    await user.click(screen.getByRole('button', { name: '완료' }));
    expect(screen.getByRole('list', { name: '등록한 리마인드 시각' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: '수정하기' }));
    expect(onSubmit).toHaveBeenLastCalledWith(
      expect.objectContaining({ remindAts: [expect.stringMatching(/T\d{2}:\d{2}:00$/)] }),
    );

    await user.click(screen.getByRole('button', { name: /리마인드 삭제/ }));
    await user.click(screen.getByRole('button', { name: '수정하기' }));
    expect(onSubmit).toHaveBeenLastCalledWith(expect.objectContaining({ remindAts: [] }));
  });

  test.each([
    ['제목', 'notice-title-field'],
    ['내용', 'notice-content-field'],
  ])('입력에 따라 글자 수가 표시된다', async (label, testId) => {
    const user = userEvent.setup();
    render(
      <NoticeForm
        initialValues={{ title: '공지', content: '내용' }}
        submitLabel="수정하기"
        onSubmit={jest.fn()}
      />,
    );

    const field = within(screen.getByTestId(testId));
    const textbox = field.getByRole('textbox', { name: label });
    const countOptions = { normalizer: (text: string) => text.split('/')[0].trim() };

    expect(field.getByText(2, countOptions)).toBeVisible();
    await user.type(textbox, '공부');
    expect(field.getByText(4, countOptions)).toBeVisible();

    await user.clear(textbox);
    expect(field.getByText(0, countOptions)).toBeVisible();
    await user.type(textbox, '치킨 먹을게요');
    expect(field.getByText(7, countOptions)).toBeVisible();
  });
});
