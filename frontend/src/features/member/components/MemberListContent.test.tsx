import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { InviteLinkBox } from './MemberListContent';

describe('초대 링크 테스트', () => {
  test('버튼을 클릭하면 초대 링크가 복사된다', async () => {
    const user = userEvent.setup();

    const writeText = jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();

    render(<InviteLinkBox />);

    const copyButton = screen.getByRole('button', {
      name: '링크 복사',
    });

    await user.click(copyButton);

    // 이후에 API 값으로 전환
    expect(writeText).toHaveBeenCalledWith('chongchong.app/join/S1');

    writeText.mockRestore();
  });
});
