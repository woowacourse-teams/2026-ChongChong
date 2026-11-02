import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { AssignmentFormValues } from '../../types';
import AssignmentForm from '../AssignmentForm';

const initialValues = {
  title: '과제',
  content: '내용',
  submissionMethod: '링크 제출',
  closeAt: '2999-12-31T23:59:59',
  submissionTarget: 'MEMBERS_AND_LEADER',
  submissionVisibility: 'LEADER_ONLY',
} satisfies AssignmentFormValues;

describe('AssignmentForm 테스트', () => {
  test('여러 리마인드 중 선택한 시각만 삭제하고 나머지 시각을 제출한다', async () => {
    const user = userEvent.setup();
    const onSubmit = jest.fn();
    render(
      <AssignmentForm
        initialValues={{
          ...initialValues,
          remindAts: ['2999-12-30T18:00:00', '2999-12-31T18:00:00'],
        }}
        submitLabel="과제 수정하기"
        onSubmit={onSubmit}
      />,
    );

    expect(
      screen.getByText('설정한 시각마다 제출하지 않은 스터디원에게 알림을 보내드릴게요'),
    ).toBeVisible();
    await user.click(screen.getByRole('button', { name: '2999년 12월 30일 18:00 리마인드 삭제' }));
    await user.click(screen.getByRole('button', { name: '과제 수정하기' }));
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ remindAts: ['2999-12-31T18:00:00'] }),
    );
  });

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

  test('저장된 제출물 공개 여부를 선택하고 변경할 수 있다', async () => {
    const user = userEvent.setup();
    const onSubmit = jest.fn();
    render(
      <AssignmentForm
        initialValues={initialValues}
        submitLabel="과제 수정하기"
        onSubmit={onSubmit}
      />,
    );

    const visibilityGroup = screen.getByRole('radiogroup', { name: '제출물 공개 여부' });
    const publicRadio = within(visibilityGroup).getByRole('radio', { name: '공개' });

    expect(publicRadio.closest('label')).toHaveAttribute('for', publicRadio.id);
    expect(within(visibilityGroup).getByRole('radio', { name: '비공개' })).toBeChecked();

    await user.click(publicRadio);
    await user.click(screen.getByRole('button', { name: '과제 수정하기' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ submissionVisibility: 'ALL_STUDY_MEMBERS' }),
    );
  });
});
