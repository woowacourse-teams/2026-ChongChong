import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AssignmentSubmissionForm from '../AssignmentSubmissionForm';

describe('AssignmentSubmissionForm 글자 수 표시', () => {
  test('입력에 따라 글자 수가 표시된다', async () => {
    const user = userEvent.setup();
    render(
      <AssignmentSubmissionForm
        initialValues={{ content: '제출', link: 'https://example.com' }}
        onSubmit={jest.fn()}
      />,
    );

    const field = within(screen.getByTestId('assignment-submission-content-field'));
    const textbox = field.getByRole('textbox', { name: '내용' });
    const countOptions = { normalizer: (text: string) => text.split('/')[0].trim() };
    expect(field.getByText(2, countOptions)).toBeVisible();

    await user.type(textbox, '공부');
    expect(field.getByText(4, countOptions)).toBeVisible();

    await user.clear(textbox);
    expect(field.getByText(0, countOptions)).toBeVisible();

    await user.type(textbox, '치킨 먹을게요');
    expect(field.getByText(7, countOptions)).toBeVisible();
  });

  test('내용 입력은 10000자로 제한된다', () => {
    render(<AssignmentSubmissionForm onSubmit={jest.fn()} />);

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    fireEvent.change(contentInput, { target: { value: '안'.repeat(10001) } });

    expect(contentInput).toHaveValue('안'.repeat(10000));
  });

  test('링크 입력은 10000자로 제한된다', () => {
    render(<AssignmentSubmissionForm onSubmit={jest.fn()} />);

    const linkInput = screen.getByRole('textbox', { name: '링크' });
    const urlPrefix = 'https://example.com/';
    fireEvent.change(linkInput, {
      target: { value: urlPrefix + 'a'.repeat(10001 - urlPrefix.length) },
    });

    expect(linkInput).toHaveValue(urlPrefix + 'a'.repeat(10000 - urlPrefix.length));
  });
});
