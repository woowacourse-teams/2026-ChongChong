import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ToastProvider } from '../../../../shared/providers/ToastProvider';
import { InviteLinkBox } from '../InviteStudyLinkBox';

const INVITE_LINK = 'https://chongchong.app/join?token=mock-token';

function setupInviteLinkBox() {
  const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
  const writeText = jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();

  render(<InviteLinkBox title="스터디 초대" inviteLink={INVITE_LINK} />, {
    wrapper: ToastProvider,
  });

  return { user, writeText };
}

describe('초대 링크 복사', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('복사 성공 표시 중에는 버튼이 비활성화되어 다시 복사되지 않는다', async () => {
    const { user, writeText } = setupInviteLinkBox();
    const copyButton = screen.getByRole('button', { name: '링크 복사' });

    await user.click(copyButton);

    await waitFor(() => expect(copyButton).toBeDisabled());
    expect(writeText).toHaveBeenCalledWith(INVITE_LINK);

    await user.click(copyButton);

    expect(writeText).toHaveBeenCalledTimes(1);
  });

  test('복사 성공 표시가 1초 뒤 사라지면 다시 복사할 수 있다', async () => {
    const { user, writeText } = setupInviteLinkBox();
    const copyButton = screen.getByRole('button', { name: '링크 복사' });

    await user.click(copyButton);
    await waitFor(() => expect(copyButton).toBeDisabled());

    act(() => jest.advanceTimersByTime(1000));

    expect(copyButton).toBeEnabled();
    await user.click(copyButton);
    expect(writeText).toHaveBeenCalledTimes(2);
  });

  test('복사에 실패하면 오류를 표시하고 다시 복사할 수 있다', async () => {
    const { user, writeText } = setupInviteLinkBox();
    writeText.mockRejectedValueOnce(new Error('클립보드 쓰기 실패'));
    const copyButton = screen.getByRole('button', { name: '링크 복사' });

    await user.click(copyButton);

    expect(writeText).toHaveBeenCalledWith(INVITE_LINK);
    expect(await screen.findByRole('status')).toHaveTextContent('링크를 복사하지 못했어요');
    expect(copyButton).toBeEnabled();

    await user.click(copyButton);

    expect(writeText).toHaveBeenCalledTimes(2);
    await waitFor(() => expect(copyButton).toBeDisabled());
  });
});
