import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createTextWithLength, insertTextInMiddle } from '../../../../test/input';
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

  test('빈 내용에 10000자를 초과한 값을 입력하면 기존 값을 유지한다', () => {
    render(<AssignmentSubmissionForm onSubmit={jest.fn()} />);

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    fireEvent.change(contentInput, { target: { value: createTextWithLength(10001) } });

    expect(contentInput).toHaveValue('');
  });

  test('내용이 10000자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
    const originalValue = createTextWithLength(10000);
    render(
      <AssignmentSubmissionForm
        initialValues={{ content: originalValue, link: '' }}
        onSubmit={jest.fn()}
      />,
    );

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    insertTextInMiddle(contentInput, originalValue);

    expect(contentInput).toHaveValue(originalValue);
  });

  test('내용의 중간 삽입 결과가 10000자 이하면 입력을 반영한다', () => {
    const originalValue = createTextWithLength(9999);
    render(
      <AssignmentSubmissionForm
        initialValues={{ content: originalValue, link: '' }}
        onSubmit={jest.fn()}
      />,
    );

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    const insertedValue = insertTextInMiddle(contentInput, originalValue);

    expect(contentInput).toHaveValue(insertedValue);
  });

  test('빈 링크에 10000자를 초과한 값을 입력하면 기존 값을 유지한다', () => {
    render(<AssignmentSubmissionForm onSubmit={jest.fn()} />);

    const linkInput = screen.getByRole('textbox', { name: '링크' });
    const urlPrefix = 'https://example.com/';
    const overLimitValue = urlPrefix + 'a'.repeat(10000 - urlPrefix.length) + 'z';
    fireEvent.change(linkInput, { target: { value: overLimitValue } });

    expect(linkInput).toHaveValue('');
  });

  test('링크가 10000자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
    const urlPrefix = 'https://example.com/';
    const originalValue = urlPrefix + 'a'.repeat(10000 - urlPrefix.length - 1) + 'z';
    render(
      <AssignmentSubmissionForm
        initialValues={{ content: '', link: originalValue }}
        onSubmit={jest.fn()}
      />,
    );

    const linkInput = screen.getByRole('textbox', { name: '링크' });
    insertTextInMiddle(linkInput, originalValue, 'X');

    expect(linkInput).toHaveValue(originalValue);
  });

  test('링크의 중간 삽입 결과가 10000자 이하면 입력을 반영한다', () => {
    const urlPrefix = 'https://example.com/';
    const originalValue = urlPrefix + 'a'.repeat(9999 - urlPrefix.length - 1) + 'z';
    render(
      <AssignmentSubmissionForm
        initialValues={{ content: '', link: originalValue }}
        onSubmit={jest.fn()}
      />,
    );

    const linkInput = screen.getByRole('textbox', { name: '링크' });
    const insertedValue = insertTextInMiddle(linkInput, originalValue, 'X');

    expect(linkInput).toHaveValue(insertedValue);
  });
});
