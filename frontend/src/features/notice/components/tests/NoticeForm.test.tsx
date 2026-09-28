import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import NoticeForm from '../NoticeForm';

describe('NoticeForm 테스트', () => {
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
