import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createWrapper } from '../../../test/render';
import StudyJoinPage from './StudyJoinPage';

describe('스터디 참가 폼 테스트', () => {
  test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const button = screen.getByRole('button', { name: '스터디 참여하기' });

    expect(button).toBeDisabled();
  });

  test('입력이 유효하면 버튼은 활성화 된다', async () => {
    const user = userEvent.setup();
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const nameInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(nameInput, '씨없는팀으로 초대합니다');
    const button = screen.getByRole('button', { name: '스터디 참여하기' });
    expect(button).toBeEnabled();
  });
});
