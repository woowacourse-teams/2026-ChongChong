import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { createWrapper } from '../../../test/render';
import StudyForm from './StudyForm';

describe('StudyForm 테스트', () => {
  test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
    render(<StudyForm />, { wrapper: createWrapper() });

    const button = screen.getByRole('button', { name: '스터디 만들기' });

    expect(button).toBeDisabled();
  });

  test('입력이 유효하면 버튼은 활성화 된다', async () => {
    const user = userEvent.setup();
    render(<StudyForm />, { wrapper: createWrapper() });

    const nameInput = screen.getByRole('textbox', { name: '스터디 이름' });
    await user.type(nameInput, '치킨');
    const button = screen.getByRole('button', { name: '스터디 만들기' });
    expect(button).toBeEnabled();
  });
});
