import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InviteLinkBox from '../InviteLinkBox';

describe('초대 링크 테스트', () => {
  test('버튼을 클릭하면 초대 링크가 복사된다', async () => {
    const user = userEvent.setup();
    const writeText = jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
    render(<InviteLinkBox title="some" inviteLink={'mock-chongchong-invite-link123'} />);
    const copyButton = await screen.findByRole('button', {
      name: '링크 복사',
    });
    await user.click(copyButton);

    expect(writeText).toHaveBeenCalledWith('mock-chongchong-invite-link123');
    writeText.mockRestore();
  });
});
