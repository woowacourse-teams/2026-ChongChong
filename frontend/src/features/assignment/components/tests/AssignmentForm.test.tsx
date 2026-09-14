import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { AssignmentValue } from '../../types';
import AssignmentForm from '../AssignmentForm';

const initialValues = {
  title: '과제',
  content: '내용',
  submissionMethod: '링크 제출',
  closeAt: '2999-12-31T23:59:59',
} satisfies AssignmentValue;

describe('AssignmentForm 테스트', () => {
  test.each([
    ['제목', 'assignment-title-field'],
    ['내용', 'assignment-content-field'],
  ])('입력에 따라 글자 수가 표시된다', async (label, testId) => {
    const user = userEvent.setup();
    render(
      <AssignmentForm
        initialValues={initialValues}
        submitLabel="과제 수정하기"
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
